# 🩺 NefroScan - Plataforma Médica Móvil e Inteligente

* **Equipo responsable:** Equipo 3
* **Puerto de la Interfaz web:** 8301

**NefroScan** es un sistema nativo desarrollado en Kotlin para dispositivos Android, concebido como una herramienta tecnológica de apoyo en la detección temprana del riesgo de Enfermedad Renal Crónica (ERC) y Nefropatía Mesoamericana. El proyecto está diseñado para asistir tanto a personas que viven en zonas rurales aledañas con escasa o nula conectividad a internet como a aquellas en zonas urbanas, implementando sincronización inteligente *Offline-First*.

---

## 👥 Integrantes del Grupo
1. Fernando José Pérez Hernández
2. Gabriela Alejandra Villalta Belloso
3. Daniela Esmeralda Mejía Belloso
4. Marelyn Guadalupe Martínez Rojas
5. Clara Dolores Hernández Hernández

---

## 🏛️ Información Institucional
- **Institución:** Instituto Nacional de San Miguel Tepezontes
- **Especialidad:** Desarrollo de Software
- **Año:** 3º Año de Bachillerato (2026)
- **Proyecto:** NefroScan

---

## 🔑 Usuarios y Perfiles de Acceso
El sistema no cuenta con un usuario o contraseña predefinido estático. Al iniciar la plataforma con la base de datos vacía, los usuarios tienen total libertad de registrarse y crear sus cuentas seleccionando entre los **3 perfiles disponibles**:
* **Promotor de Salud:** Gestión de tamizajes en campo y triaje de riesgo.
* **Doctor:** Centro analítico, diagnósticos con IA, dictámenes SOAP y modelo 3D.
* **Paciente:** Consulta de expedientes locales y autocuidado.

---

## 🚀 Arquitectura y Características Principales

1. **Panel del Promotor de Salud (Tamizaje de Campo):**
   * Evaluación de presión arterial sistólica y factor de exposición laboral agrícola.
   * Algoritmo de triaje por semáforo de riesgo (Verde / Amarillo / Rojo).
   * Monitoreo de sincronización *Offline-First* adaptado para zonas rurales sin internet, con rutas de control y alertas tempranas.

2. **Panel Médico (IA, Visión por Computadora & Modelos Matemáticos):**
   * **Centro Analítico y Tablero Clínico:** Monitoreo hemodinámico, proyecciones de Tasa de Filtración Glomerular estimada (eGFR) frente a daño parenquimatoso y distribución por estadios renales (G1 a G5).
   * **Inferencia Local (*On-Device*):** Análisis automatizado de ecografías renales mediante visión por computadora en TensorFlow Lite (256x256).
   * **Modelos Predictivos Integrados:** Implementación de calculadoras clínicas como CKD-EPI (eGFR), KFRE (Riesgo de Diálisis), nefrotoxicidad y prescriptor de hidratación.
   * **Dictamen SOAP:** Generación y exportación automatizada de notas médicas estructuradas (Subjetivo, Objetivo, Análisis, Plan) en PDF.
   * **Gemelo Digital 3D:** Visualización anatómica del riñón con SceneView, mapas de calor y ajuste temporal multivariable.
   * **Radar Epidemiológico Nacional:** Mapa geográfico de concentración de riesgo por sectores.
   * **Pasaporte Clínico QR:** Expedientes mediante códigos QR para consulta rápida sin conexión.

3. **Panel del Paciente:**
   * Consulta de expedientes locales mediante Room Database.
   * Herramientas de autocuidado, recordatorios y asistente virtual conversacional (chatbot educativo).

---

## 🛠️ Librerías y Dependencias del Proyecto

El proyecto utiliza **Gradle 8+** (Kotlin DSL `build.gradle.kts`):

| Categoría | Librería / Artefacto | Versión | Propósito Técnico |
| :--- | :--- | :---: | :--- |
| **UI & Base** | `androidx.core:core-ktx` | *Catalog* | Extensiones KTX para la API nativa de Android. |
| **Componentes Visuales**| `com.google.android.material:material` | *Catalog* | Componentes gráficos de Material Design 3. |
| **Inteligencia Artificial**| `org.tensorflow:tensorflow-lite` | `2.14.0` | Inferencia local On-Device (`.tflite`). |
| **Soporte IA** | `org.tensorflow:tensorflow-lite-support` | `0.4.4` | Preprocesamiento de tensores de imágenes. |
| **Motor 3D & AR** | `io.github.sceneview:sceneview` | `2.2.1` | Renderizado del modelo 3D anatómico (`.glb`). |
| **Base de Datos Local** | `androidx.room:room-runtime` | `2.6.1` | Persistencia offline-first sobre SQLite. |
| **Room Corrutinas** | `androidx.room:room-ktx` | `2.6.1` | Consultas asíncronas reactivas. |
| **Compilador Room** | `androidx.room:room-compiler` | `2.6.1` | Procesador de anotaciones `kapt`. |
| **Plataforma Nube** | `com.google.firebase:firebase-bom` | `33.1.2` | Gestor centralizado de Firebase. |
| **Nube NoSQL** | `com.google.firebase:firebase-firestore-ktx` | *BoM* | Sincronización en tiempo real. |

---

## 🐳 Cómo ejecutar el proyecto con Docker

1. Asegúrate de tener **Docker** y **Docker Compose** instalados en tu computadora.
2. Abre una terminal en la carpeta raíz del proyecto.
3. Ejecuta el siguiente comando para construir y levantar los contenedores:
   ```bash
   docker compose up -d --build
