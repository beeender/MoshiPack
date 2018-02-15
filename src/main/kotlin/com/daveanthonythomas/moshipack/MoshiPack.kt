package com.daveanthonythomas.moshipack

import com.squareup.moshi.*
import okio.Buffer
import okio.BufferedSource
import java.lang.reflect.Type

class MoshiPack(private var builder: Moshi.Builder.() -> kotlin.Unit = {},
                var moshi: Moshi = MoshiPack.moshi(builder)) {

    companion object {
        inline fun <reified T> pack(value: T, moshi: Moshi): BufferedSource =
                Buffer().also { moshi.adapter<T>(T::class.java).toJson(MsgpackWriter(it), value) }

        inline fun <reified T> pack(value: T, crossinline builder: Moshi.Builder.() -> Unit = {}): BufferedSource =
               pack(value, moshi(builder))

        inline fun <reified T> unpack(source: BufferedSource, moshi: Moshi): T  {
            val type: Type = object : TypeReference<T>() {}.type
            var adapter: JsonAdapter<T>
            try {
                // TODO: Map<Any, Any> & List<Any> is causing a crash in Moshi, investigate this.
                adapter = moshi.adapter<T>(type)
            } catch (e: IllegalArgumentException) {
                // Fallback fixes Map<Any, Any>/List<Any> crash
                adapter = moshi.adapter<T>(T::class.java)
            }
            return adapter.fromJson(MsgpackReader(source)) as T
        }

        inline fun <reified T> unpack(source: BufferedSource, crossinline builder: Moshi.Builder.() -> Unit = {}): T =
                unpack(source, moshi(builder))

        inline fun <reified T> unpack(bytes: ByteArray, moshi: Moshi): T =
                unpack(Buffer().apply { write(bytes) }, moshi)

        inline fun <reified T> unpack(bytes: ByteArray, crossinline builder: Moshi.Builder.() -> Unit = {}): T =
                unpack(Buffer().apply { write(bytes) }, builder)

        inline fun moshi(crossinline  builder: Moshi.Builder.() -> Unit = {}) =
                Moshi.Builder().apply(builder).build()

        fun msgpackToJson(bytes: ByteArray): String = msgpackToJson(Buffer().apply { write(bytes) })
        fun msgpackToJson(source: BufferedSource) = FormatInterchange(Format.Msgpack(), Format.Json())
                        .transform(source).readUtf8()
        fun jsonToMsgpack(jsonString: String) = jsonToMsgpack(Buffer().apply { writeUtf8(jsonString) })
        fun jsonToMsgpack(source: BufferedSource): BufferedSource = FormatInterchange(Format.Json(), Format.Msgpack())
                        .transform(source)
    }

    inline fun <reified T> pack(value: T) = MoshiPack.pack(value, moshi)
    inline fun <reified T> packToByteArray(value: T): ByteArray = pack(value).readByteArray()
    inline fun <reified T> unpack(bytes: ByteArray): T = unpack(Buffer().apply { write(bytes) })
    inline fun <reified T> unpack(source: BufferedSource): T = MoshiPack.unpack(source, moshi)
}