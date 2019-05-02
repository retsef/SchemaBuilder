import androidx.sqlite.db.SupportSQLiteDatabase

class SchemaBuilder(private val db: SupportSQLiteDatabase, var table_name: String) {
    var schema: String = ""
    val columns: MutableList<Column> = mutableListOf()
    val relationships: MutableList<Relation> = mutableListOf()

    init {
        fetchSchema()

        fetchColumns()
        fetchRelations()
    }

    fun generateCreateSchema(_columns: List<Column> = columns, _relationships: List<Relation> = relationships): String {
        return "CREATE TABLE `$table_name` ${generateSchemaInfo(_columns, _relationships)}"
    }

    fun generateSchemaInfo(_columns: List<Column> = columns, _relationships: List<Relation> = relationships): String {
        return "(${_columns.joinToString(", ")}, " +
                "${columns.filter { it.pk == 1 }.map { it.foreignKeyString() }.joinToString(",")} " +
                "${if(_relationships.isEmpty()) "" else ","} ${_relationships.joinToString(",")})"
    }

    fun generateSchemaColumns(_columns: List<Column> = columns): String {
        return _columns.map { it.name }.joinToString(", ")
    }



    private fun fetchSchema() {
        db.query("select sql from sqlite_master where name = \"$table_name\"").apply {
            moveToFirst()
            schema = getString(0)
            close()
        }
    }

    private fun fetchColumns() {
        db.query("PRAGMA table_info(\"$table_name\")").apply {
            try {
                val _indexOfName = getColumnIndexOrThrow("name")
                val _indexOfType = getColumnIndexOrThrow("type")
                val _indexOfNotNull = getColumnIndexOrThrow("notnull")
                val _indexOfPk = getColumnIndexOrThrow("pk")

                while (moveToNext()) {
                    columns.add(Column(
                        name = getString(_indexOfName),
                        type = getString(_indexOfType),
                        notnull = getInt(_indexOfNotNull),
                        pk = getInt(_indexOfPk)
                    ))
                }
            } catch (e: IllegalArgumentException) { e.printStackTrace() }

            close()
        }

    }

    private fun fetchRelations() {
        db.query("PRAGMA foreign_key_list(\"$table_name\")").apply {
            try {
                val _indexOfTable = getColumnIndexOrThrow("table")
                val _indexOfFrom = getColumnIndexOrThrow("from")
                val _indexOfTo = getColumnIndexOrThrow("to")
                val _indexOfOnUpdate = getColumnIndexOrThrow("on_update")
                val _indexOfOnDelete = getColumnIndexOrThrow("on_delete")
                val _indexOfMatch = getColumnIndexOrThrow("match")

                while (moveToNext()) {
                    relationships.add(Relation(
                        table = getString(_indexOfTable),
                        from = getString(_indexOfFrom),
                        to = getString(_indexOfTo),
                        on_update = getString(_indexOfOnUpdate),
                        on_delete = getString(_indexOfOnDelete),
                        match = getString(_indexOfMatch)
                    ))
                }
            } catch (e: IllegalArgumentException) { e.printStackTrace() }

            close()
        }
    }


    data class Column(
        var name: String,
        var type: String,
        var notnull: Int = 0,
        var pk: Int = 0
    ) {
        override fun toString() = "`$name` $type ${if(notnull != 0) "NOT NULL" else "" }"

        fun foreignKeyString(): String? {
            if(pk == 0) return null
            return "PRIMARY KEY(`$name`)"
        }
    }

    data class Relation(
            var table: String,
            var from: String,
            var to: String,
            var on_update: String,
            var on_delete: String,
            var match: String
    ) {
        override fun toString() = "FOREIGN KEY(`$from`) REFERENCES `$table`(`$to`) ON UPDATE $on_update ON DELETE $on_delete"
    }

}
