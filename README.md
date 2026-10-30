# Meny Touch Lock 1.0

App Android independiente de bloqueo táctil, desarrollada desde cero en Java. Gratis, sin compras dentro de la app, sin anuncios, sin telemetría y sin permiso de Internet. No reutiliza código ni recursos de Bloqueo Táctil (la aplicación enviada como referencia).

## Funciones

- Botón flotante arrastrable que aparece sobre videos y otras aplicaciones.
- Tocar el botón activa una capa de bloqueo táctil de pantalla completa.
- Desbloqueo con pulsación prolongada (1 a 5 segundos), anillo de progreso.
- Acción de emergencia **Desbloquear** y **Detener** desde la notificación.
- Retardo de activación configurable (inmediato, 3, 5 o 10 segundos). Puedes cancelar durante la cuenta atrás tocando de nuevo el botón.
- Fondo semitransparente con opacidad ajustable de 0% a 60%.
- Vibración y ayudas visuales opcionales.
- Opción para evitar el reposo de pantalla mientras está bloqueada.
- Tema oscuro y marca MenyTheProducer.

## Compilar e instalar en Android

1. Instala **Android Studio** reciente y el SDK de Android 35 (si el IDE lo solicita).
2. Abre la carpeta `MenyTouchLock` como proyecto, espera a que Gradle sincronice. En Android Studio con Gradle instalado puedes usar la opción de "Repair Gradle Wrapper" si el IDE la solicita; la descarga de Gradle 8.9 y del Android Gradle Plugin necesita conexión a Internet.
3. Selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El instalador generado estará en `app/build/outputs/apk/debug/app-debug.apk`.
5. Instálalo en tu teléfono; concede el permiso **Mostrar sobre otras apps** desde la propia aplicación.
6. Toca **Activar botón flotante**, abre tu video, toca el candado flotante para bloquear y mantén el dedo sobre el candado central para desbloquear.

La app funciona en Android 8 o posterior (API 26+). Su comportamiento final debe verificarse en un dispositivo Android real, especialmente en modelos que restringen las apps en segundo plano.

### Alternativa de compilación en GitHub Actions

El proyecto incluye `.github/workflows/build-apk.yml`. Si lo subes a un repositorio GitHub, abre **Actions > Compilar APK gratis > Run workflow**. Cuando finalice, descarga el artefacto `MenyTouchLock-APK` (contiene `app-debug.apk`). Esto requiere una cuenta GitHub y una conexión a Internet, pero no una suscripción de la aplicación.

## Seguridad y restricciones del sistema

- Android no permite que una app ordinaria bloquee por completo los controles del sistema, el botón de encendido, los botones físicos, ni todos los gestos de navegación. Esto es intencional por seguridad.
- Si el teléfono termina automáticamente los servicios en segundo plano, agrega Meny Touch Lock a la lista de apps sin restricciones de batería.
- Si pierdes la capacidad de desbloquear desde el círculo, usa la acción **Desbloquear** en la notificación o fuerza la detención de la app desde Ajustes.
- Este proyecto no necesita permisos de accesibilidad, acceso a medios, contactos, cámara ni ubicación.

## Privacidad

Los ajustes se guardan solo de forma local en `SharedPreferences`. La app no tiene permiso `INTERNET`, no integra redes publicitarias ni código de pagos.

## Estado

Se entrega el **código fuente completo**, no una APK ya compilada, porque en este entorno no está disponible el Android SDK ni las herramientas de compilación y firma necesarias. Está listo para abrirse en Android Studio. No se asegura que esté probado en tu dispositivo.
