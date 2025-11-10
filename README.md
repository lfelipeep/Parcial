# Parcial

#  Sistema de Gestión de Biblioteca – Proyecto Parcial Final

Este proyecto es un **sistema de gestión de biblioteca** hecho en **Java**, diseñado para aplicar los conceptos de:

- Programación Orientada a Objetos (OOP)  
- Tipos primitivos vs clases wrapper  
- Manejo de excepciones personalizadas  
- Validaciones y reglas de negocio  

---

##  **Objetivo del sistema**
Simular el funcionamiento de una biblioteca pública que gestiona:

- Libros (registro, disponibilidad, préstamos)
- Usuarios (registro, control de préstamos, multas)
- Préstamos (control de fechas, devoluciones y multas)
- Reportes básicos (usuarios con multas)

---

##  **Cómo ejecutar el programa**

### Requisitos previos
- Tener **Java JDK 17 o superior** instalado.  
  Verifica con:
  ```bash
  java -version
  javac -version

### Ejecutar en Visual Studio Code (o terminal)

1)Guarda el archivo del proyecto como Parcial.java.

2)Abre la carpeta donde está guardado el archivo.

3)En la terminal, escribe los siguientes comandos:
 ```
javac Parcial.java
java Parcial
 ```
Esto compilará y ejecutará el programa.

#Estructura interna del programa

El sistema está hecho en un solo archivo (Parcial.java), pero contiene varias clases que se comunican entre sí.

#Reglas de negocio implementadas
Al ejecutar el programa, se muestra un menú con las siguientes opciones:

1. Agregar libro
2. Registrar usuario
3. Realizar préstamo
4. Devolver libro
5. Ver libros
6. Ver préstamos de usuario
7. Ver usuarios con multas
9. Salir

#Flujo paso a paso del programa

###Ejemplo rápido de uso
```
=== Sistema de Gestión de Biblioteca ===
1. Agregar libro
2. Registrar usuario
...

> Opción: 1
Título: El Principito
Autor: Antoine de Saint-Exupéry
Año: 1943
Ejemplares: 3
Libro agregado con ISBN automático: 1000000000001

> Opción: 2
Nombre: Luis
Email: luis@gmail.com
Usuario registrado con ID: 1

> Opción: 3
ID usuario: 1
ISBN libro: 1000000000001
Préstamo creado con ID: 1
```
