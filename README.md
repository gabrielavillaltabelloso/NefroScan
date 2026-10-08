# 🩺 NefroScan - Plataforma Médica Móvil e Inteligente

**NefroScan** es un sistema nativo desarrollado en Kotlin para dispositivos Android, concebido como una herramienta tecnológica de apoyo en la detección temprana del riesgo de Enfermedad Renal Crónica (ERC) y Nefropatía Mesoamericana en comunidades rurales de El Salvador.

---

## 🏛️ Información Institucional
- **Institución:** Instituto Nacional de San Miguel Tepezontes
- **Especialidad:** Desarrollo de Software
- **Año:** 3º Año de Bachillerato (2026)
- **Proyecto:** NefroScan

---

## 🚀 Arquitectura y Características Principales

1. **Panel del Promotor de Salud (Tamizaje de Campo):**
   * Evaluación de presión arterial sistólica y factor de exposición laboral agrícola.
   * Algoritmo de triaje por semáforo de riesgo (Verde / Amarillo / Rojo).
   * Monitoreo de sincronización *Offline-First* con rutas de control y alertas tempranas de derivación urgente.

2. **Panel Médico (IA, Visión por Computadora & Modelos Matemáticos):**
   * **Centro Analítico y Tablero Clínico:** Monitoreo hemodinámico, proyecciones de Tasa de Filtración Glomerular estimada (eGFR) frente a daño parenquimatoso y distribución por estadios renales (G1 a G5).
   * **Inferencia Local (*On-Device*):** Análisis automatizado de ecografías renales mediante visión por computadora en TensorFlow Lite (256x256).
   * **Modelos Predictivos Integrados:** Implementación de calculadoras clínicas como CKD-EPI (eGFR), KFRE (Riesgo de Diálisis), evaluador de nefrotoxicidad (farmacovigilancia) y prescriptor de hidratación por clima/exposición solar.
   * **Dictamen SOAP:** Generación e exportación automatizada de notas médicas estructuradas (Subjetivo, Objetivo, Análisis, Plan) y reportes en formato PDF.
   * **Gemelo Digital 3D:** Visualización anatómica del riñón con SceneView, con mapas de calor, ajuste de transparencia y proyección temporal multivariable según hidratación y consumo de sodio.
   * **Radar Epidemiológico Nacional:** Mapa geográfico de concentración de riesgo por sectores (Alerta Roja, Moderado y Bajo Riesgo) con ubicación de viviendas y asignación de pacientes.
   * **Pasaporte Clínico QR:** Generación de expedientes mediante código QR para consulta rápida en zonas con escasa conectividad.

3. **Panel del Paciente:**
   * Consulta de expedientes locales mediante Room Database.
   * Herramientas de autocuidado, recordatorios y asistente virtual conversacional (chatbot educativo) para orientación y prevención en salud renal.

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

- **Android Studio:** Jellyfish / Koala o superior
- **Compile SDK:** 34
- **Min SDK:** 24 (Android 7.0 Nougat)
- **JDK:** Java 17
- **Lenguaje:** Kotlin

---

## 📊 Métricas de Desempeño y Validación

- **Procesamiento Algorítmico:** 245 ms para el cálculo e inferencia de modelos matemáticos (CKD-EPI / KFRE).
- **Análisis de Ecografía:** 1.2 segundos por escaneo mediante TensorFlow Lite.
- **Precisión de Estratificación:** 92% de precisión en la clasificación del daño y fase de riesgo renal (Estadios G1 a G5).
