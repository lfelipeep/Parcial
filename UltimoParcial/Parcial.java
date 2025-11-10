

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;



class LibroNoDisponibleException extends Exception {
    public LibroNoDisponibleException(String message) { super(message); }
}

class UsuarioSinCupoException extends Exception {
    public UsuarioSinCupoException(String message) { super(message); }
}

class ValidacionException extends RuntimeException {
    public ValidacionException(String message) { super(message); }
}


enum EstadoPrestamo {
    ACTIVO, DEVUELTO, VENCIDO
}



class Libro {
    private static final java.util.concurrent.atomic.AtomicLong isbnCounter = new java.util.concurrent.atomic.AtomicLong(1000000000000L);

    private final String isbn;
    private final String titulo;
    private final String autor;
    private final int anio;
    private final int ejemplaresTotales;
    private final AtomicInteger ejemplaresDisponibles;

    public Libro(String titulo, String autor, int anio, int ejemplaresTotales) {
        if (anio < 1000 || anio > LocalDate.now().getYear())
            throw new ValidacionException("Año inválido.");
        if (ejemplaresTotales <= 0)
            throw new ValidacionException("Ejemplares totales deben ser > 0.");

        this.isbn = String.valueOf(isbnCounter.incrementAndGet());
        this.titulo = titulo;
        this.autor = autor;
        this.anio = anio;
        this.ejemplaresTotales = ejemplaresTotales;
        this.ejemplaresDisponibles = new AtomicInteger(ejemplaresTotales);
    }

    public String getIsbn() { return isbn; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public int getAnio() { return anio; }
    public int getEjemplaresDisponibles() { return ejemplaresDisponibles.get(); }

    public boolean estaDisponible() { return ejemplaresDisponibles.get() > 0; }

    public void prestar() throws LibroNoDisponibleException {
        int prev;
        do {
            prev = ejemplaresDisponibles.get();
            if (prev <= 0)
                throw new LibroNoDisponibleException("No hay ejemplares disponibles de: " + titulo);
        } while (!ejemplaresDisponibles.compareAndSet(prev, prev - 1));
    }

    public void devolver() {
        int prev;
        do {
            prev = ejemplaresDisponibles.get();
            if (prev >= ejemplaresTotales) return;
        } while (!ejemplaresDisponibles.compareAndSet(prev, prev + 1));
    }

    @Override
    public String toString() {
        return String.format("%s — %s (%d) — ISBN:%s — Disponibles:%d/%d",
                titulo, autor, anio, isbn, ejemplaresDisponibles.get(), ejemplaresTotales);
    }
}



class Usuario {
    private final int id;
    private final String nombre;
    private final String email;
    private final List<Integer> prestamosIds = new ArrayList<>();
    private BigDecimal multas = BigDecimal.ZERO;

    public static final int MAX_LIBROS_PRESTADOS = 3;
    public static final BigDecimal MULTA_MAX = new BigDecimal("5000");

    public Usuario(int id, String nombre, String email) {
        if (nombre == null || nombre.trim().isEmpty())
            throw new ValidacionException("Nombre requerido.");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$"))
            throw new ValidacionException("Email inválido.");
        this.id = id;
        this.nombre = nombre.trim();
        this.email = email.trim();
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public BigDecimal getMultas() { return multas; }

    public boolean puedePedirPrestado() {
        return prestamosIds.size() < MAX_LIBROS_PRESTADOS && multas.compareTo(MULTA_MAX) <= 0;
    }

    public void agregarPrestamo(int id) { prestamosIds.add(id); }
    public void removerPrestamo(int id) { prestamosIds.remove(Integer.valueOf(id)); }

    public void agregarMulta(BigDecimal monto) {
        multas = multas.add(monto);
        if (multas.compareTo(MULTA_MAX) > 0)
            throw new ValidacionException("La multa supera el máximo permitido.");
    }

    @Override
    public String toString() {
        return String.format("Usuario %d: %s — %s — Multas: %s — Libros prestados: %d",
                id, nombre, email, multas, prestamosIds.size());
    }
}



class Prestamo {
    private final int id;
    private final int usuarioId;
    private final String isbnLibro;
    private final LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion;
    private final LocalDate fechaLimite;
    private EstadoPrestamo estado;

    public static final int DIAS_PRESTAMO = 14;
    public static final BigDecimal MULTA_POR_DIA = new BigDecimal("500");

    public Prestamo(int id, int usuarioId, String isbnLibro) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.isbnLibro = isbnLibro;
        this.fechaPrestamo = LocalDate.now();
        this.fechaLimite = fechaPrestamo.plusDays(DIAS_PRESTAMO);
        this.estado = EstadoPrestamo.ACTIVO;
    }

    public int getId() { return id; }
    public int getUsuarioId() { return usuarioId; }
    public String getIsbnLibro() { return isbnLibro; }
    public EstadoPrestamo getEstado() { return estado; }

    public BigDecimal calcularMulta() {
        LocalDate ref = (fechaDevolucion != null) ? fechaDevolucion : LocalDate.now();
        long dias = ChronoUnit.DAYS.between(fechaLimite, ref);
        return dias > 0 ? MULTA_POR_DIA.multiply(BigDecimal.valueOf(dias)) : BigDecimal.ZERO;
    }

    public void marcarDevuelto() {
        if (estado == EstadoPrestamo.DEVUELTO) return;
        fechaDevolucion = LocalDate.now();
        BigDecimal multa = calcularMulta();
        estado = multa.compareTo(BigDecimal.ZERO) > 0 ? EstadoPrestamo.VENCIDO : EstadoPrestamo.DEVUELTO;
    }

    @Override
    public String toString() {
        return String.format("Préstamo %d — Usuario:%d — Libro:%s — Estado:%s",
                id, usuarioId, isbnLibro, estado);
    }
}



class Biblioteca {
    private final Map<String, Libro> libros = new HashMap<>();
    private final Map<Integer, Usuario> usuarios = new HashMap<>();
    private final Map<Integer, Prestamo> prestamos = new HashMap<>();
    private final AtomicInteger usuarioSeq = new AtomicInteger(1);
    private final AtomicInteger prestamoSeq = new AtomicInteger(1);

    public void agregarLibro(Libro libro) { libros.putIfAbsent(libro.getIsbn(), libro); }

    public Usuario registrarUsuario(String nombre, String email) {
        int id = usuarioSeq.getAndIncrement();
        Usuario u = new Usuario(id, nombre, email);
        usuarios.put(id, u);
        return u;
    }

    public Prestamo realizarPrestamo(int usuarioId, String isbn)
            throws LibroNoDisponibleException, UsuarioSinCupoException {
        Usuario u = usuarios.get(usuarioId);
        if (u == null) throw new UsuarioSinCupoException("Usuario no encontrado.");
        if (!u.puedePedirPrestado()) throw new UsuarioSinCupoException("El usuario no puede pedir más libros.");
        Libro l = libros.get(isbn);
        if (l == null) throw new LibroNoDisponibleException("Libro no existe.");
        l.prestar();
        int id = prestamoSeq.getAndIncrement();
        Prestamo p = new Prestamo(id, usuarioId, isbn);
        prestamos.put(id, p);
        u.agregarPrestamo(id);
        return p;
    }

    public void devolverLibro(int prestamoId) {
        Prestamo p = prestamos.get(prestamoId);
        if (p == null) return;
        p.marcarDevuelto();
        Libro l = libros.get(p.getIsbnLibro());
        if (l != null) l.devolver();
        Usuario u = usuarios.get(p.getUsuarioId());
        if (u != null) u.removerPrestamo(prestamoId);
        BigDecimal multa = p.calcularMulta();
        if (u != null && multa.compareTo(BigDecimal.ZERO) > 0) u.agregarMulta(multa);
    }

    public List<Libro> listarLibros() { return new ArrayList<>(libros.values()); }
    public List<Prestamo> prestamosUsuario(int uid) {
        List<Prestamo> res = new ArrayList<>();
        for (Prestamo p : prestamos.values()) if (p.getUsuarioId() == uid) res.add(p);
        return res;
    }
    public List<Usuario> usuariosConMultas() {
        List<Usuario> r = new ArrayList<>();
        for (Usuario u : usuarios.values()) if (u.getMultas().compareTo(BigDecimal.ZERO) > 0) r.add(u);
        return r;
    }
}



public class Parcial {
    public static void main(String[] args) {
        Biblioteca biblioteca = new Biblioteca();
        Scanner sc = new Scanner(System.in);
        boolean run = true;
        System.out.println("=== Sistema de Gestión de Biblioteca ===");

        while (run) {
            System.out.println("""
                    1. Agregar libro
                    2. Registrar usuario
                    3. Realizar préstamo
                    4. Devolver libro
                    5. Ver libros
                    6. Ver préstamos de usuario
                    7. Ver usuarios con multas
                    8. Salir
                    """);
            System.out.print("Opción: ");
            String op = sc.nextLine();

            try {
                switch (op) {
                    case "1" -> {
                        System.out.print("Título: "); String titulo = sc.nextLine();
                        System.out.print("Autor: "); String autor = sc.nextLine();
                        System.out.print("Año: "); int anio = Integer.parseInt(sc.nextLine());
                        System.out.print("Ejemplares: "); int ej = Integer.parseInt(sc.nextLine());
                        Libro nuevo = new Libro(titulo, autor, anio, ej);
                        biblioteca.agregarLibro(nuevo);
                        System.out.println("Libro agregado con ISBN automático: " + nuevo.getIsbn());
                    }
                    case "2" -> {
                        System.out.print("Nombre: "); String n = sc.nextLine();
                        System.out.print("Email: "); String e = sc.nextLine();
                        Usuario u = biblioteca.registrarUsuario(n, e);
                        System.out.println("Usuario registrado con ID: " + u.getId());
                    }
                    case "3" -> {
                        System.out.print("ID usuario: "); int uid = Integer.parseInt(sc.nextLine());
                        System.out.print("ISBN libro: "); String isbn = sc.nextLine();
                        Prestamo p = biblioteca.realizarPrestamo(uid, isbn);
                        System.out.println("Préstamo creado con ID: " + p.getId());
                    }
                    case "4" -> {
                        System.out.print("ID préstamo: "); int pid = Integer.parseInt(sc.nextLine());
                        biblioteca.devolverLibro(pid);
                        System.out.println("Devolución procesada.");
                    }
                    case "5" -> biblioteca.listarLibros().forEach(System.out::println);
                    case "6" -> {
                        System.out.print("ID usuario: "); int uid = Integer.parseInt(sc.nextLine());
                        biblioteca.prestamosUsuario(uid).forEach(System.out::println);
                    }
                    case "7" -> biblioteca.usuariosConMultas().forEach(System.out::println);
                    case "8" -> run = false;
                    default -> System.out.println("Opción no válida.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Programa finalizado.");
        sc.close();
    }
}
