package com.mongodb.stitch.android.services.mongodb.performance

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.mongodb.embedded.client.MongoClientSettings
import com.mongodb.embedded.client.MongoClients
import com.mongodb.embedded.client.MongoEmbeddedSettings
import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.where
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.codecs.configuration.CodecRegistries
import org.bson.types.ObjectId
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception

@RunWith(AndroidJUnit4::class)
class CrudPerformanceTests {

    companion object {
        private val runId by lazy {
            ObjectId()
        }

        private val crudHarness by lazy {
            CRUDPerformanceIntTestsHarness()
        }

        private val mongoClient by lazy {
            try {
                MongoClients.close()
            } catch (ex: Exception) {}

            try {
                MongoClients.init(MongoEmbeddedSettings.builder().build())
            } catch (ex: Exception) {}

            val mongoClientSettings = MongoClientSettings.builder()
                .dbPath(String.format("%s/%s", System.getProperty("java.io.tmpdir"), "tests"))
                .codecRegistry(CodecRegistries.fromCodecs(TestObjectCodec()))
                .build()

            MongoClients.create(mongoClientSettings)
        }
    }

    // Tests for Mongo insert
    @Test
    fun testMongoInsert() {
        var mongoColl = mongoClient.getDatabase("performance").getCollection("realm", TestObject::class.java)
        var objectsToInsert = mutableListOf<TestObject>()

        // Insert Test
        crudHarness.runPerformanceTestWithParams(
            "CrudTest: Mongo-Insert", "mongo", "insert", runId,
            beforeEach = { numDocs ->
                mongoColl.drop()
                mongoColl = mongoClient.getDatabase("performance").getCollection("realm", TestObject::class.java)
                val count = mongoColl.count()
                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(count.toInt(), 0, "Collection not emptied")

                // Generate documents to insert
                objectsToInsert = (0..(numDocs-1)).map {
                    TestObject(ObjectId().toHexString(), it, byteArrayOf(1, 2, 3), RealmList(4, 5, 6))
                }.toMutableList()
            },
            testDefinition = { start, numDocs ->
                mongoColl.insertMany(objectsToInsert.subList(start, start + numDocs))
            },
            afterEach = { numDocs ->
                val count = mongoColl.count()
                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(count.toInt(), numDocs, "Collection not emptied")
            })
    }

    @Test
    fun testMongoDelete() {

        var mongoColl = mongoClient.getDatabase("performance").getCollection("realm", TestObject::class.java)
        var objectsToInsert = mutableListOf<TestObject>()

        // Delete Test
        crudHarness.runPerformanceTestWithParams(
            "CrudTest: Mongo-Delete", "mongo", "delete", runId,
            beforeEach = { numDocs ->
                mongoColl.drop()
                mongoColl = mongoClient.getDatabase("performance").getCollection("realm", TestObject::class.java)
                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(mongoColl.count().toInt(), 0, "Not Empty (1)")

                // Generate documents to insert
                objectsToInsert = (0..(numDocs-1)).map {
                    TestObject(ObjectId().toHexString(), it, byteArrayOf(1, 2, 3), RealmList(4, 5, 6))
                }.toMutableList()
                mongoColl.insertMany(objectsToInsert)

                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(mongoColl.count().toInt(), numDocs, "Not Filled")
            },
            testDefinition = { start, numDocs ->
                val doc = BsonDocument("\$gte", BsonInt32(start)).append("\$lt", BsonInt32(start + numDocs))
                mongoColl.deleteMany(BsonDocument("foo", doc))
            },
            afterEach = { numDocs ->
                val count = mongoColl.count()
                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(count.toInt(), 0, "Not Empty (2)")
            })
    }

    @Test
    fun testRealmInsert() {
        Realm.init(InstrumentationRegistry.getContext())
        val realm = Realm.getDefaultInstance()

        var objectsToInsert = mutableListOf<TestObject>()
        crudHarness.runPerformanceTestWithParams(
            "CrudTest: Realm-Insert", "realm", "insert", runId,
            beforeEach = { numDocs ->
                realm.beginTransaction()
                realm.deleteAll()
                realm.commitTransaction()
                val realmQuery = realm.where<TestObject>()
                SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(realmQuery.count().toInt(), 0, "Collection not emptied")

                objectsToInsert = (0..(numDocs-1)).map {
                    TestObject(ObjectId().toHexString(), it, byteArrayOf(1, 2, 3), RealmList(4, 5, 6))
                }.toMutableList()
            },
            testDefinition = { start, numDocs ->
                // Generate documents to insert
                val newRealm = Realm.getDefaultInstance()
                newRealm.beginTransaction()
                newRealm.insert(objectsToInsert.subList(start, start + numDocs).toList())
                newRealm.commitTransaction()
            },
            afterEach = { numDocs ->
                realm.beginTransaction()
                realm.deleteAll()
                realm.commitTransaction()
            })

    }
}

