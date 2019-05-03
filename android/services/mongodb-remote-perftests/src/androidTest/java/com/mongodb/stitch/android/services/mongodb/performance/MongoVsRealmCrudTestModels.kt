package com.mongodb.stitch.android.services.mongodb.performance

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

@RealmClass
open class TestObject constructor(
    @PrimaryKey
    var id: String = "",
    var foo: Int = 0,
    var bar: ByteArray = byteArrayOf(),
    var baz: RealmList<Long> = RealmList()
) : RealmObject()

class TestObjectCodec : Codec<TestObject> {
    override fun getEncoderClass(): Class<TestObject> {
        return TestObject::class.java
    }

    override fun encode(
        writer: BsonWriter?,
        value: TestObject?,
        encoderContext: EncoderContext?
    ) {
        if (value != null && writer != null) {
            writer.writeStartDocument()
            writer.writeString("_id", value.id)
            writer.writeInt32("foo", value.foo)
            writer.writeBinaryData("bar", BsonBinary(BsonBinarySubType.BINARY, value.bar))
            writer.writeInt32("__count", value.baz.size)
            writer.writeStartArray("baz")
            value.baz.forEach(writer::writeInt64)
            writer.writeEndArray()
            writer.writeEndDocument()
        }
    }

    override fun decode(
        reader: BsonReader?,
        decoderContext: DecoderContext?
    ): TestObject? {
        return if (reader == null)
            null
        else {
            TestObject(
                reader.readString("_id"),
                reader.readInt32("foo"),
                reader.readBinaryData("bar").data,
                RealmList(*reader.readInt32("__count").let {
                    reader.readStartArray()
                    (0..it).map { reader.readInt64() }.toTypedArray()
                }))
        }
    }
}