# 🩺 NefroScan - Plataforma Médica Móvil e Inteligente

**NefroScan** es un sistema nativo desarrollado en Kotlin para dispositivos Android, concebido como una herramienta tecnológica de apoyo en la detección temprana del riesgo de Enfermedad Renal Crónica (ERC) y Nefropatía Mesoamericana en comunidades rurales de El Salvador.

---

## 🏛️ Información Institucional
* **Institución:** Instituto Nacional de San Miguel Tepezontes
* **Especialidad:** Desarrollo de Software
* **Año:** 3º Año de Bachillerato (2026)
* **Proyecto:** NefroScan

---

## 🚀 Arquitectura y Características Principales

1. **Panel del Promotor de Salud (Tamizaje de Campo):**
   * Evaluación de presión arterial sistólica y factor de exposición laboral agrícola.
   * Algoritmo de triaje por semáforo de riesgo (Verde / Amarillo / Rojo).
   * Monitoreo de sincronización *Offline-First*.
   
2. **Panel Médico (IA & Visión por Computadora):**
   * Inferencia local (*On-Device*) con TensorFlow Lite (256x256) para análisis ecográfico.
   * Renderizado y visualización anatómica 3D del Gemelo Digital del riñón con SceneView.
   * Generación de Pasaporte Clínico mediante código QR.

3. **Panel del Paciente:**
   * Consulta de expedientes locales mediante Room Database.
   * Asistente virtual conversacional para orientación y prevención en salud renal.

---

## 🛠️ Librerías y Dependencias del Proyecto

El proyecto está configurado bajo **Gradle 8+** (Kotlin DSL `build.gradle.kts`). A continuación se detallan las librerías principales integradas en la aplicación:

| Categoría | Librería / Artefacto | Versión | Propósito Técnico |
| :--- | :--- | :---: | :--- |
| **UI & Base** | `androidx.core:core-ktx` | *Catalog* | Extensiones KTX de Kotlin para la API nativa de Android. |
| **Componentes Visuales**| `com.google.android.material:material` | *Catalog* | Componentes gráficos de Material Design 3. |
| **Inteligencia Artificial**| `org.tensorflow:tensorflow-lite` | `2.14.0` | Inferencia local On-Device de modelos convolucionales (`.tflite`). |
| **Soporte IA** | `org.tensorflow:tensorflow-lite-support` | `0.4.4` | Preprocesamiento y transformación de tensores de imágenes. |
| **Motor 3D & AR** | `io.github.sceneview:sceneview` | `2.2.1` | Renderizado y manipulación del modelo 3D anatómico (`.glb`). |
| **Base de Datos Local** | `androidx.room:room-runtime` | `2.6.1` | Persistencia local offline-first sobre SQLite. |
| **Room Corrutinas** | `androidx.room:room-ktx` | `2.6.1` | Consultas asíncronas reactivas sin congelar el hilo principal. |
| **Compilador Room** | `androidx.room:room-compiler` | `2.6.1` | Procesador de anotaciones `kapt` para verificación SQL en compilación. |
| **Plataforma Nube** | `com.google.firebase:firebase-bom` | `33.1.2` | Gestor centralizado de versiones de Google Firebase. |
| **Nube NoSQL** | `com.google.firebase:firebase-firestore-ktx` | *BoM* | Sincronización en tiempo real con Firebase Cloud. |
| **Asincronía** | `kotlinx-coroutines-android` | `1.7.3` | Manejo de hilos de ejecución en segundo plano. |

---

## 📋 Requisitos del Entorno de Desarrollo

* **Android Studio:** Jellyfish / Koala o superior
* **Compile SDK:** 34
* **Min SDK:** 24 (Android 7.0 Nougat)
* **JDK:** Java 17
* **Lenguaje:** Kotlin
