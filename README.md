# 🚕 Taximeter Technical Challenge

This is a production-ready implementation of the Taximeter technical challenge, built following Clean Architecture principles, MVVM, and a fully extensible design.

## ✨ Key Features

* **Multi-Module Architecture:** The project is decoupled into `:app`, `:data`, and `:domain` modules for a clean separation of concerns.
* **Real-time Fare Calculation:** The fare is calculated in time based on distance (km) and time (seconds) using `StateFlow` and `combine`.
* **100% Offline (Cache-First):** The app uses Room as a cache. It only calls the API on the first run. If the API fails or the device is offline, the app continues to function perfectly with the last known configuration.
* **Extensible Design (Strategy Pattern):** Fulfills the "senior" requirement. Adding new supplements (like a "Baby Seat") is trivial and does *not* require modifying the main `ViewModel` logic.
* **Gestión de Errores:** La UI maneja los estados de `Loading` y `Error` (con botón de reintento) si la configuración de precios no se puede obtener.
* **Simulación de Viaje:** Incluye 3 rutas de GPS falsas (`Route1`, `Route2`, `Route3`) y dos velocidades (`Default` y `Fast`) para pruebas, tal como se pedía.

---

## 🏗️ Arquitectura

El proyecto sigue los principios de **Clean Architecture** y **MVVM**.

### `:domain`
A pure Kotlin (`kotlin-jvm`) module. It contains the core business models (e.g., `PriceConfig`), repository interfaces (`TaximeterRepository`), and the business logic interfaces (e.g., `SupplementStrategy`). It has no knowledge of Android, Room, or Retrofit.

### `:data`
An Android library module that contains the implementation details.
* **`TaximeterRepositoryImpl`**: Implements the "Cache-First" logic.
* **Network (Retrofit & Moshi):** Handles fetching the remote price configuration.
* **Database (Room):** Caches the price configuration for offline use.
* **Strategies (`LuggageStrategy`):** Concrete implementations of the Strategy Pattern.
* **`LocationProvider`**: The fake GPS simulator.

### `:app`
The main Android Application module (UI layer).
* **Hilt:** Used for Dependency Injection across the entire app (with KSP).
* **`TaximeterViewModel`**: The "brain" of the UI. It orchestrates business logic and exposes a single, reactive `StateFlow` (`uiState`).
* **`TaximeterScreen` (Compose):** The "dumb" view that observes the `uiState` and recomposes accordingly.

---

## 🚀 Requisito Clave: Extensibilidad

> "Añadir un nuevo suplemento (ej: "Silla de Bebé") debe ser simple."

Esta solución ataca la extensibilidad en dos niveles:

### 1. Lógica (Patrón Strategy)
La lógica de cálculo **no está "hardcodeada"** en el `ViewModel`. Se usa el **Patrón Strategy** a través de Hilt:

1.  El `ViewModel` recibe un `Set<SupplementStrategy>` inyectado.
2.  Cada `SupplementStrategy` (como `LuggageStrategy`) define su propio `id` ("luggage") y su lógica de cálculo (`count * 5.0`).
3.  El `ViewModel` simplemente itera sobre este `Set` para calcular el precio total.

**Para añadir una "Silla de Bebé (3€)":**
1.  Crear la clase `InfantSeatStrategy` (con `id = "infant_seat"` y `calculate { count * 3.0 }`).
2.  Añadirla al `StrategyModule.kt` de Hilt con `@Binds @IntoSet`.
*(El `ViewModel` no necesita cambios para calcular el nuevo precio).*

### 2. UI (Suplementos Dinámicos)
La UI (`TaximeterScreen`) **tampoco está "hardcodeada"**.
1.  El `ViewModel` usa el `Set<SupplementStrategy>` para construir dinámicamente la lista de `SupplementUiModel` que se envía a la vista.
2.  La `TaximeterScreen` simplemente usa un `forEach` para renderizar *cualquier* suplemento que reciba en la lista.

**Para añadir la "Silla de Bebé" a la UI:**
1.  (Después de hacer el Paso 1 de Lógica) Solo hay que añadir el nombre de UI al `supplementDisplayNames` map en el `ViewModel`.

**La UI (`TaximeterScreen.kt`) y el `ViewModel` (`combine`, `createActiveUiState`, etc.) no requieren ningún otro cambio.**

---

## 🧪 Testing

El proyecto está "blindado" con una suite de tests que valida la lógica de negocio y de datos.

### 1. `TaximeterViewModelTest` (Unit Test - `app/src/test`)
* Prueba el "cerebro" (`ViewModel`) usando `MockK` y `Turbine`.
* Valida 5 escenarios:
    * ✅ **Loading State:** Confirms `isLoadingConfig` is true on startup.
    * ✅ **Error State:** Confirms `isConfigError` is true if the API fails.
    * ✅ **Scenario 1 (Full):** 11km/1800s + 1 Luggage = **€25.20**
    * ✅ **Scenario 2 (No Luggage):** 11km/1800s + 0 Luggage = **€20.20**
    * ✅ **Scenario 3 (Short Ride):** Route2 + 1 Luggage = **~€5.60**

### 2. `TaximeterRepositoryImplTest` (Instrumentation Test - `app/src/androidTest`)
* Prueba la lógica "Cache-First" del repositorio.
* Usa `Room.inMemoryDatabaseBuilder` (para BBDD falsa) y `MockWebServer` (para API falsa).
* Valida los 5 escenarios:
    * ✅ Cache is Empty (calls API)
    * ✅ Cache is Full (does NOT call API)
    * ✅ 404 Error (API failure)
    * ✅ Malformed JSON (API failure)
    * ✅ Network Timeout (API failure)