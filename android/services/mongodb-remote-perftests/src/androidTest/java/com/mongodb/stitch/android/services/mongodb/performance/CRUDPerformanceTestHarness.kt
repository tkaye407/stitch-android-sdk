package com.mongodb.stitch.android.services.mongodb.performance

import android.util.Log
import com.mongodb.stitch.android.testutils.BaseStitchAndroidIntTest

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.bson.BsonInt32
import org.bson.Document
import org.bson.types.ObjectId
import kotlin.system.measureTimeMillis

typealias CrudTestDefinition = (start: Int, numDocs: Int) -> Unit
typealias CrudBeforeBlock = (numDocs: Int) -> Unit
typealias CrudAfterBlock = (numDocs: Int) -> Unit

class CRUDPerformanceIntTestsHarness : BaseStitchAndroidIntTest() {

    fun logMessage(message: String) {
        if (SyncPerformanceTestUtils.shouldOutputToStdOut()) {
            Log.d("PerfLog", message)
        }
    }

    private fun runTestDefinition(test: CrudTestDefinition, start: Int, numDocs: Int) = object : Thread() {
        override fun run() {
            test(start, numDocs)
        }
    }

    fun runPerformanceTestWithParams(
        testName: String,
        storageType: String,
        crudOp: String,
        runId: ObjectId,
        testDefinition: CrudTestDefinition,
        beforeEach: CrudBeforeBlock = { _ -> },
        afterEach: CrudAfterBlock = { _ -> }
    ) {
        val testParams = TestParams(runId, testName)
        val oneThreadResults = mutableListOf<Document>()
        val twoThreadResults = mutableListOf<Document>()

        logMessage(String.format("Starting Test: %s", testName))

        for (numDoc in SyncPerformanceTestUtils.getNumDocs()) {
            val oneThreadResult = CrudResult(numDoc)
            val twoThreadResult = CrudResult(numDoc)

            for (iter in 1..SyncPerformanceTestUtils.getNumIters()) {
                logMessage("Testing (numDocs: $numDoc, iter: $iter)")
                try {

                    // Run the test with one thread
                    beforeEach(numDoc)
                    var time = measureTimeMillis {
                        testDefinition(0, numDoc)
                    }
                    oneThreadResult.runTimes.add(time.toDouble())
                    afterEach(numDoc)

                    // Run the test with two threads
                    beforeEach(numDoc)
                    val numEach = numDoc / 2
                    val job1 = runTestDefinition(testDefinition, 0, numEach)
                    val job2 = runTestDefinition(testDefinition, numEach, numEach)
                    time = measureTimeMillis {
                        job1.start()
                        job2.start()
                        job1.join()
                        job2.join()
                    }
                    twoThreadResult.runTimes.add(time.toDouble())
                    afterEach(numDoc)

                } catch (e: Exception) {
                    val failureMessage = e.localizedMessage ?: e.toString()
                    logMessage("Failure: $failureMessage")
                    throw e
                }
            }
            logMessage("One Thread:  ${oneThreadResult.asBson.toJson()}")
            logMessage("Two Threads: ${twoThreadResult.asBson.toJson()}")
            oneThreadResults.add(oneThreadResult.asBson)
            twoThreadResults.add(twoThreadResult.asBson)
        }

        val doc = testParams.asBson
        doc["storage"] = storageType
        doc["crudOp"] = crudOp
        doc.remove("date")

        // Send one thread results
        doc["results"] = oneThreadResults.toList()
        doc["numThreads"] = 1
        sendJsonToWebhook(doc.toJson())

        // Send two thread results
        doc["results"] = twoThreadResults.toList()
        doc["numThreads"] = 2
        sendJsonToWebhook(doc.toJson())
    }

    val webhookUrl = "https://webhooks.mongodb-stitch.com/api/client/v2.0/app/android-sdk-perf-testing-yuvef/service/Hook/incoming_webhook/addResult"
    val JSON = MediaType.get("application/json; charset=utf-8")
    private fun sendJsonToWebhook(json: String) {
        try {
            val client = OkHttpClient()
            val body = RequestBody.create(JSON, json)
            val request = Request.Builder().url(webhookUrl).post(body).build()
            client.newCall(request).execute()
        } catch (ex: Exception) {
            logMessage(ex.localizedMessage)
        }
    }
}


open class CrudResult(numDocs: Int) {
    val runTimes = arrayListOf<Double>()
    var failures = arrayListOf<FailureResult>()
    val numOutliers = SyncPerformanceTestUtils.getNumOutliers()

    val asBson by lazy {
        if (failures.size < (SyncPerformanceTestUtils.getNumIters() + 1) / 2) {
            Document(
                mapOf(
                    "numDocs" to BsonInt32(numDocs),
                    "success" to true,
                    "timeMs" to DataBlock(runTimes.toDoubleArray(), numOutliers).toBson(),
                    "numFailures" to failures.size,
                    "failures" to failures.map { it.asBson }
                )
            )
        } else {
            Document(
                mapOf(
                    "numDocs" to BsonInt32(numDocs),
                    "success" to false,
                    "numFailures" to failures.size,
                    "failures" to failures.map { it.asBson }
                )
            )
        }
    }
}
