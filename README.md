# 🪙 AA1 Mining Pool

Este proyecto implementa una simulación de un **Pool de Minería de Criptomonedas** utilizando una arquitectura Cliente-Servidor en Java.

El sistema simula la distribución de bloques de transacciones, cálculo de Hashes mediante algoritmos concurrentes y validación centralizada.


## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 21.
* **UI Framework:** JavaFX 21 (con FXML).
* **Comunicación:** Java Sockets (TCP/IP).
* **Algoritmo de Hash:** MD5.
* **IDE Recomendado:** IntelliJ IDEA.

## 🚀 Características Principales

El proyecto cumple con los requisitos avanzados de la práctica:

### Funcionalidades Obligatorias:
* Generar un paquete de datos aleatorios: El servidor generará un “paquete” de datos, simulando X transacciones. Dichas transacciones tienen un formato similar a un movimiento de una cuenta origen a una destino de una cantidad dada.
* Los clientes son capaces de conectarse y desconectarse del servidor: El servidor lleva una lista de las conexiones actuales.
* El servidor gestiona de manera concurrente las conexiones de los clientes: Gestiona el envío y la recepción de la información de manera concurrente
* Los clientes son capaces de aceptar las peticiones de minado y ejecutar la búsqueda del valor: Una vez encontrado, si lo han encontrado, son capazes de enviar el valor al servidor
* El servidor valida la solución aportada: Una vez validada, finalizará el proceso en el resto de hilos

### Funcionalidades Opcionales:
* Implementa un mecanismo concurrente para la búsqueda de soluciones (2 puntos): Con los algoritmos y métodos vistos en clase, los clientes calculen el resultado de forma concurrente.
* Diseño de UI para el servidor (2 puntos): Diseño de un UI para el servidor que muestra las conexiones actuales y, los paquetes que va a mandar, las soluciones encontradas…
* Implementa una fase de negociación en la que el servidor establece la dificultar del minado (1 punto): El servidor establece el número de ceros a conseguir.


---

## ⚙️ Instalación y Configuración Previa

El proyecto utiliza **JavaFX 21** (y no viene incluido por defecto en el JDK), es necesario configurar la librería externa.

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/Mri186481/MiningPool
    ```
2.  **Descargar JavaFX SDK:**
    * Descarga el SDK de JavaFX 21 desde [GluonHQ](https://gluonhq.com/products/javafx/).
    * Descomprímelo en una ruta local (ej: `C:\LibreriasJava\javafx-sdk-21`).
3.  **Configurar en IntelliJ IDEA:**
    * Ve a `File` > `Project Structure` > `Libraries`.
    * Añade (`+`) la carpeta `lib` del SDK de JavaFX que acabas de descargar.
    * Asegúrate de que la librería se aplica al módulo del servidor (`server-pool`).

---

## ▶️ Cómo Ejecutar el Proyecto

### 1. Iniciar el Servidor
El servidor debe iniciarse primero. Debido a la configuración de JavaFX, se utiliza una clase lanzadera para evitar errores de módulos.

* **Clase Principal:** `ServerLauncher.java` (ubicada en `server-pool/src/`).
* **Puerto por defecto:** `3000`.

**Pasos:**
1.  Abre `ServerLauncher.java`.
2.  Ejecuta el archivo (Run).
3.  Aparecerá una ventana negra ("Monitor de Minería") esperando conexiones.

### 2. Iniciar el Cliente (Minero)
Puedes iniciar tantos clientes como quieras para simular múltiples mineros compitiendo.

* **Clase Principal:** `Main.java` (ubicada en `client-miner/src/`).

**Pasos:**
1.  Abre `Main.java`.
2.  Ejecuta el archivo.
3.  Verás en la consola que detecta tus núcleos y comienza a minar cuando recibe trabajo.

---

## 📡 Protocolo de Comunicación

El sistema utiliza un protocolo de texto simple:

1.  **Conexión:** Cliente envía `connect` -> Servidor responde `ack`.
2.  **Nueva Ronda:** Servidor envía `new_request <dificultad> <rango> <datos>`.
    * *Ejemplo:* `new_request 4 0-100000 mv|100|userA|userB;`
3.  **Minería:** Cliente confirma con `ack` y comienza a buscar el hash.
4.  **Solución:** Si encuentra el hash, Cliente envía `sol <numero>`.
5.  **Fin de Ronda:** Servidor valida y envía `end <info_ganador>` a todos los clientes para que detengan sus hilos.

## 👥 Autor

* **[Miguel Angel Rubio Ibor, alumno de 2º de Desarrollo de Aplicaciones Multiplataforma de San Valero]** 

---