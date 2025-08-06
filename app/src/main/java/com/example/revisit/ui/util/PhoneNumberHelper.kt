package com.example.revisit.util // Ajusta a tu estructura de paquetes

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log // Conservado para errores críticos
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.util.Locale

object PhoneNumberHelper {

    private const val TAG = "PhoneNumberHelper" // Útil para los logs que se conservan
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * Intenta obtener el código ISO del país del dispositivo.
     * Puede ser útil como `defaultRegion` si los números no tienen código de país.
     */
    fun getDeviceCountryCode(context: Context): String? {
        // NUEVA LÓGICA: Priorizar el Locale del sistema
        val locale = Locale.getDefault()
        val localeCountry = locale.country
        Log.d("DeviceCountryCode", "Locale Country (Prioritized): '$localeCountry' (from Locale: ${locale.displayName})")
        if (localeCountry.isNotBlank() && localeCountry.length == 2) {
            return localeCountry.uppercase(Locale.US)
        }

        // Fallback a SIM/Network si el Locale no da un país válido
        // (Esta parte es opcional, podrías decidir devolver null si el Locale no es suficiente)
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.let { tm ->
                val simCountry = tm.simCountryIso
                Log.d("DeviceCountryCode", "SIM Country ISO (Fallback): '$simCountry'")
                if (simCountry != null && simCountry.length == 2) {
                    return simCountry.uppercase(Locale.US)
                }

                val networkCountry = tm.networkCountryIso
                Log.d("DeviceCountryCode", "Network Country ISO (Fallback): '$networkCountry'")
                if (networkCountry != null && networkCountry.length == 2) {
                    return networkCountry.uppercase(Locale.US)
                }
            }
        } catch (e: SecurityException) {
            Log.w("DeviceCountryCode", "SecurityException al acceder a TelephonyManager (Fallback): ${e.message}")
        } catch (e: Exception) {
            Log.w("DeviceCountryCode", "Excepción al acceder a TelephonyManager (Fallback): ${e.message}")
        }

        Log.w("DeviceCountryCode", "No se pudo determinar un código de país válido.")
        return null // O un valor por defecto si lo prefieres
    }

    /**
     * Parsea un string de número de teléfono a un objeto PhoneNumber.
     * @param phoneNumberString El número de teléfono como string.
     * @param defaultRegion El código ISO de país de 2 letras (ej. "US", "MX") para usar si
     *                      el número no tiene un prefijo de país internacional.
     * @return Objeto PhoneNumber si el parseo es exitoso, null en caso contrario.
     */
    private fun parsePhoneNumber(phoneNumberString: String, defaultRegion: String?): PhoneNumber? {
        return try {
            // Asegurarse de que defaultRegion, si es provisto, sea válido para libphonenumber
            val regionToUse = if (defaultRegion?.length == 2) defaultRegion.uppercase(Locale.US) else null
            phoneUtil.parse(phoneNumberString, regionToUse)
        } catch (e: NumberParseException) {
            // Este log es útil si los números fallan al parsear de forma inesperada.
            Log.e(TAG, "NumberParseException para '$phoneNumberString' con región '$defaultRegion'. Error: ${e.errorType} - ${e.message}")
            null
        }
    }

    /**
     * Verifica si un número de teléfono es válido.
     * @param phoneNumberString El número de teléfono como string.
     * @param defaultRegion El código ISO del país por defecto.
     * @return true si el número es válido, false en caso contrario.
     */
    fun isValidPhoneNumber(phoneNumberString: String, defaultRegion: String?): Boolean {
        val numberProto = parsePhoneNumber(phoneNumberString, defaultRegion)
        return numberProto != null && phoneUtil.isValidNumber(numberProto)
    }

//    /**
//     * Formatea un número de teléfono al formato E.164 (ej. "+12223334444").
//     * @param phoneNumberString El número de teléfono como string.
//     * @param defaultRegion El código ISO del país por defecto.
//     * @return El número en formato E.164 si es válido y parseable, null en caso contrario.
//     */
//    fun formatToE164(phoneNumberString: String, defaultRegion: String?): String? {
//        val numberProto = parsePhoneNumber(phoneNumberString, defaultRegion)
//        return if (numberProto != null && phoneUtil.isValidNumber(numberProto)) {
//            phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164)
//        } else {
//            null
//        }
//    }

    /**
     * Obtiene un número formateado adecuado para ser marcado por el sistema (ACTION_DIAL)
     * o para enviar SMS (smsto:).
     * Siempre devuelve el formato E.164 si el número es válido.
     * Si no es válido o no se puede parsear, devuelve una versión limpiada del original.
     *
     * @param phoneNumberString El número de teléfono como string.
     * @param defaultRegion El código ISO del país por defecto.
     * @return Un string del número formateado listo para marcar/SMS, o el original limpiado si no se puede mejorar.
     */
    fun getNumberForDialingOrSms(phoneNumberString: String, defaultRegion: String?): String {
        val numberProto = parsePhoneNumber(phoneNumberString, defaultRegion)

        if (numberProto != null && phoneUtil.isValidNumber(numberProto)) {
            // Siempre devolver E.164 para consistencia con el marcador.
            return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164)
        }

        // Fallback: si no se pudo parsear o no es válido, limpiar el string original.
        // Esto es un último recurso.
        Log.w(TAG, "getNumberForDialingOrSms: No se pudo formatear a E.164 o el número no es válido ('$phoneNumberString', región '$defaultRegion'). Devolviendo versión limpiada.")
        return phoneNumberString.filter { it.isDigit() || it == '+' }
    }

//    /**
//     * Obtiene el número de teléfono formateado específicamente para el intent de WhatsApp (`wa.me/`).
//     * Espera el formato E.164 pero sin el signo '+'.
//     *
//     * @param phoneNumberString El número de teléfono como string.
//     * @param defaultRegion El código ISO del país por defecto.
//     * @return Un string del número para WhatsApp (solo dígitos, ej. "12223334444") si es válido,
//     *         o un string vacío si no se puede formatear correctamente o el original no es válido.
//     */
//    fun getNumberForWhatsApp(phoneNumberString: String, defaultRegion: String?): String {
//        val e164Number = formatToE164(phoneNumberString, defaultRegion)
//        // Quita el '+' y devuelve vacío si e164Number es null (porque formatToE164 devuelve null para inválidos)
//        return e164Number?.removePrefix("+") ?: ""
//    }

//    /**
//     * Obtiene una descripción del tipo de número (ej. "Móvil", "Fijo", "Número gratuito").
//     * @param context Contexto para acceder a recursos de string en el futuro (no usado actualmente para strings).
//     * @param phoneNumberString El número de teléfono como string.
//     * @param defaultRegion El código ISO del país por defecto.
//     * @return Un string descriptivo del tipo de número, o "Desconocido" si no se puede determinar.
//     */
//    fun getNumberTypeDescription(context: Context, phoneNumberString: String, defaultRegion: String?): String {
//        val numberProto = parsePhoneNumber(phoneNumberString, defaultRegion)
//        if (numberProto != null && phoneUtil.isValidNumber(numberProto)) { // Añadida verificación de validez
//            val numberType = phoneUtil.getNumberType(numberProto)
//            // Para una app real, usarías recursos de string: context.getString(R.string.mobile_number_type)
//            return when (numberType) {
//                PhoneNumberUtil.PhoneNumberType.FIXED_LINE -> "Fijo" // Reemplazar con context.getString(R.string.fixed_line)
//                PhoneNumberUtil.PhoneNumberType.MOBILE -> "Móvil"
//                PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE -> "Fijo o Móvil"
//                PhoneNumberUtil.PhoneNumberType.TOLL_FREE -> "Número gratuito"
//                PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE -> "Tarifa premium"
//                PhoneNumberUtil.PhoneNumberType.SHARED_COST -> "Coste compartido"
//                PhoneNumberUtil.PhoneNumberType.VOIP -> "VoIP"
//                PhoneNumberUtil.PhoneNumberType.PERSONAL_NUMBER -> "Número personal"
//                PhoneNumberUtil.PhoneNumberType.PAGER -> "Buscapersonas"
//                PhoneNumberUtil.PhoneNumberType.UAN -> "Número de acceso universal"
//                PhoneNumberUtil.PhoneNumberType.VOICEMAIL -> "Buzón de voz"
//                PhoneNumberUtil.PhoneNumberType.UNKNOWN -> "Desconocido"
//                else -> "Desconocido" // Cubre null o valores futuros
//            }
//        }
//        return "Desconocido"
//    }
}
