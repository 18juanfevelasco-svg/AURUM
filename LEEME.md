# Aurum: tu app de finanzas personales

Una app Android para controlar tus finanzas. Registra sola las compras que haces con tus tarjetas leyendo las notificaciones y los SMS de tu banco.

## Qué hace
- **Captura automática**: cuando llega el aviso del banco ("Compraste $45.900 en Rappi con tu T.Cred *1234"), Aurum guarda el monto, el comercio, la tarjeta, el método de pago y el banco, y le asigna una categoría.
- **Evita duplicados**: si el banco te avisa por la app y por SMS, solo se guarda una vez.
- **Inicio**: lo que llevas gastado en el mes, ingresos, balance y presupuesto, más tus categorías principales.
- **Historial**: buscador, filtros (gastos, ingresos, automáticos) y movimientos agrupados por día.
- **Análisis**: gráfica de dona por categoría, barras de los últimos 6 meses y datos útiles (mayor gasto, ticket promedio, día más costoso, ahorro).
- **Ajustes**: presupuesto, tema claro u oscuro, exportación a Excel (CSV) y un **probador del detector** donde pegas el texto de una notificación para ver qué entiende la app.

---

## Cómo convertir este proyecto en un APK (paso a paso)

### Opción A: con Android Studio (recomendada)
1. **Descarga Android Studio** desde https://developer.android.com/studio e instálalo con las opciones por defecto.
2. **Descomprime** el archivo `Aurum.zip` en una carpeta, por ejemplo `Documentos/Aurum`.
3. Abre Android Studio y elige **Open** (Abrir). Selecciona la carpeta `Aurum`, la que contiene el archivo `settings.gradle.kts`.
4. **Espera** a que termine la sincronización ("Gradle sync"). La primera vez descarga varias cosas y puede tardar entre 5 y 15 minutos. Abajo verás una barra de progreso.
   - Si te sugiere actualizar el "Android Gradle Plugin", puedes aceptar.
5. En el menú de arriba ve a **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
6. Cuando termine, aparece un aviso abajo a la derecha. Haz clic en **locate**. Tu archivo es `app-debug.apk`.
7. Pasa ese archivo a tu celular (por cable, Drive o WhatsApp a ti mismo) y ábrelo para instalarlo. Si Android lo pide, permite **"Instalar apps desconocidas"** para la app con la que lo abriste.

> También puedes conectar tu celular por cable (con la "Depuración USB" activada) y darle al botón ▶ verde: se instala directo.

### Opción B: sin instalar nada, usando GitHub
1. Crea una cuenta gratis en https://github.com y un repositorio nuevo.
2. Sube **todo el contenido** de la carpeta `Aurum` (incluida la carpeta `.github`).
3. Ve a la pestaña **Actions**. Se ejecuta solo el proceso "Compilar APK" y tarda unos 5 minutos.
4. Entra a la ejecución terminada (✓ verde) y abajo, en **Artifacts**, descarga `Aurum-apk`. Adentro está el APK.

---

## Primer uso en el celular
1. Abre Aurum, escribe tu nombre y tu presupuesto (opcional).
2. Toca **Activar** en "Acceso a notificaciones", busca **Aurum** en la lista y actívalo.
3. **Importante (Android 13 o superior):** como la app no viene de Play Store, Android puede decir que el ajuste está **"restringido"**. Para quitar el bloqueo:
   - Ve a *Ajustes → Apps → Aurum* (o toca "Abrir información de la app" dentro de Aurum).
   - Toca los **tres puntos ⋮** arriba a la derecha → **"Permitir ajustes restringidos"**.
   - Vuelve a activar el acceso a notificaciones.
4. Permite los **avisos de Aurum** para que te confirme cada gasto registrado.
5. ¿Quieres ver cómo luce con información? Entra a *Ajustes → Cargar datos de ejemplo* (después puedes borrarlos con "Borrar todos los movimientos").

### Si tu banco no se detecta
- Copia el texto de una notificación de compra y pégalo en *Ajustes → Probar detector*.
- Si dice "No se detectó", envíale ese texto a Claude para ajustar el detector.
- Mientras tanto, activa **"Escuchar todas las apps"**.

---

## Datos que guarda cada movimiento
| Dato | Ejemplo |
|---|---|
| Monto | $45.900 |
| Tipo | Gasto / Ingreso |
| Categoría | Comida |
| Comercio | Rappi |
| Método de pago | Crédito, Débito, Efectivo, Transferencia, PSE |
| Tarjeta (últimos 4) | 1234 |
| Banco | Bancolombia |
| Fecha y hora | 21 nov 2026, 8:15 p. m. |
| Origen | Automático / Manual |
| Nota | (opcional) |
| Texto original | El aviso del banco tal cual llegó |

Todo se guarda **solo en tu celular**, en una base de datos local. La app no usa internet.

## Estructura del proyecto (para quien quiera curiosear)
- `data/`: los datos (modelo, base de datos, categorías, ajustes).
- `notif/`: el lector de notificaciones del banco y los avisos.
- `ui/`: las pantallas, los colores y las animaciones.
