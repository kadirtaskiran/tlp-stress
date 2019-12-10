package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.functions.Random


class KeyValue : IStressProfile {

    lateinit var insert: PreparedStatement
    lateinit var select: PreparedStatement
    lateinit var delete: PreparedStatement


    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO keyvalue (key, datasource_partition, value) VALUES (?, ?, ?)")
        select = session.prepare("SELECT * from keyvalue WHERE key = ? and datasource_partition = ? and value = ?")
        delete = session.prepare("DELETE from keyvalue WHERE key = ? and datasource_partition = ? and value = ?")
    }

    override fun schema(): List<String> {
        val table = """CREATE TABLE IF NOT EXISTS keyvalue (
                        key text,
                        datasource_partition int,
                        value text,
                        PRIMARY KEY ((key, datasource_partition),value)
                        )""".trimIndent()
        return listOf(table)
    }

    override fun getDefaultReadRate(): Double {
        return 0.5
    }

    override fun getRunner(context: StressContext): IStressRunner {

        val value = context.registry.getGenerator("keyvalue", "value")

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val data = value.getText()

                val bound = select.bind("foo", data.hashCode()%256 , data  )
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                //val bound = insert.bind(partitionKey.getText(),  data)
                val bound = insert.bind("foo", data.hashCode()%256 , data )

                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                val bound = delete.bind("foo", data.hashCode()%256 , data )
                return Operation.Deletion(bound)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random().apply{min=100; max=200})
    }
}
