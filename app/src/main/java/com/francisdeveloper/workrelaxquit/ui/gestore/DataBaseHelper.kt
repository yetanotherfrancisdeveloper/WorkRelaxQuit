package com.francisdeveloper.workrelaxquit.ui.gestore

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.francisdeveloper.workrelaxquit.ui.home.DataModel
import com.francisdeveloper.workrelaxquit.ui.home.AccDataModel

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MyAppDatabase"
        const val TABLE_NAME = "Data"
        const val COL_ID = "id"
        const val COL_DATE = "date"
        const val COL_VALUE = "value"
        const val COL_TYPE = "type"

        const val SECOND_TABLE_NAME = "InitialData"
        const val SECOND_COL_ID = "id"
        const val COL_FERIE = "ferie"
        const val COL_PERMESSI = "permessi"
        const val COL_GIORNI_FERIE = "giorniFerie"
        const val COL_ORE_PERMESSI = "orePermessi"
        const val COL_INITIAL_DATE = "initialDate"

        const val THIRD_TABLE_NAME = "Accumulated"
        const val THIRD_COL_ID = "id"
        const val COL_ACC_FERIE = "ferie"
        const val COL_ACC_PERMESSI = "permessi"
        const val COL_ACC_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_DATE TEXT, $COL_VALUE DOUBLE, $COL_TYPE TEXT)"
        db?.execSQL(createTableQuery)
        val createSecondTableQuery = "CREATE TABLE $SECOND_TABLE_NAME ($SECOND_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FERIE DOUBLE, $COL_PERMESSI DOUBLE, $COL_GIORNI_FERIE INT, $COL_ORE_PERMESSI DOUBLE, $COL_INITIAL_DATE TEXT)"
        db?.execSQL(createSecondTableQuery)
        val createThirdTableQuery = "CREATE TABLE $THIRD_TABLE_NAME ($THIRD_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_ACC_FERIE DOUBLE, $COL_ACC_PERMESSI DOUBLE, $COL_ACC_DATE TEXT)"
        db?.execSQL(createThirdTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(dropTableQuery)

        val dropSecondTableQuery = "DROP TABLE IF EXISTS $SECOND_TABLE_NAME"
        db?.execSQL(dropSecondTableQuery)

        val dropThirdTableQuery = "DROP TABLE IF EXISTS $THIRD_TABLE_NAME"
        db?.execSQL(dropThirdTableQuery)
        onCreate(db)
    }

    fun insertData(date: String, value: Double, type: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_DATE, date)
            put(COL_VALUE, value)
            put(COL_TYPE, type)
        }
        return db.insert(TABLE_NAME, null, contentValues)
    }

    fun insertInitialData(ferie: Double, permessi: Double, giorniFerie: Int, orePermessi: Double, initialDate: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_FERIE, ferie)
            put(COL_PERMESSI, permessi)
            put(COL_GIORNI_FERIE, giorniFerie)
            put(COL_ORE_PERMESSI, orePermessi)
            put(COL_INITIAL_DATE, initialDate)
        }
        return db.insert(SECOND_TABLE_NAME, null, contentValues)
    }

    fun insertAccData(acc_ferie: Double, acc_permessi: Double, acc_date: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_ACC_FERIE, acc_ferie)
            put(COL_ACC_PERMESSI, acc_permessi)
            put(COL_DATE, acc_date)
        }
        return db.insert(THIRD_TABLE_NAME, null, contentValues)
    }

    fun getDataForMonth(month: String): List<AccDataModel> {
        val db = readableDatabase
        val query = "SELECT * FROM $THIRD_TABLE_NAME WHERE strftime('%Y-%m-%d', $COL_ACC_DATE) = ?"
        val selectionArgs = arrayOf(month)

        val cursor = db.rawQuery(query, selectionArgs)
        val dataList = mutableListOf<AccDataModel>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(THIRD_COL_ID))
                val accFerie = cursor.getDouble(cursor.getColumnIndex(COL_ACC_FERIE))
                val accPermessi = cursor.getDouble(cursor.getColumnIndex(COL_ACC_PERMESSI))
                val date = cursor.getString(cursor.getColumnIndex(COL_DATE))

                val dataModel = AccDataModel(accFerie, accPermessi, date, id)
                dataList.add(dataModel)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return dataList
    }

    fun getAllData(): Cursor? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        return db.rawQuery(query, null)
    }

    fun getAccumulatedData(): List<AccDataModel> {
        val db = readableDatabase
        val query = "SELECT * FROM $THIRD_TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        val dataList = mutableListOf<AccDataModel>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(THIRD_COL_ID))
                val accFerie = cursor.getDouble(cursor.getColumnIndex(COL_ACC_FERIE))
                val accPermessi = cursor.getDouble(cursor.getColumnIndex(COL_ACC_PERMESSI))
                val date = cursor.getString(cursor.getColumnIndex(COL_ACC_DATE))

                val accDataModel = AccDataModel(accFerie, accPermessi, date, id)
                dataList.add(accDataModel)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return dataList
    }

    fun getSumOfColumnValues(column: String): Double {
        val db = readableDatabase
        val query = "SELECT SUM($COL_VALUE) FROM $TABLE_NAME WHERE $COL_TYPE = '$column'"
        val cursor = db.rawQuery(query, null)
        var sum = 0.0
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0)
        }
        cursor.close()
        return sum
    }

    fun getSumOfAccumulatedFerie(): Double {
        val db = readableDatabase
        val query = "SELECT SUM($COL_ACC_FERIE) FROM $THIRD_TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        var sum = 0.0
        if (cursor.moveToFirst() && cursor.count > 0) {
            sum = cursor.getDouble(0)
        }
        cursor.close()
        return sum
    }

    fun getSumOfAccumulatedPermessi(): Double {
        val db = readableDatabase
        val query = "SELECT SUM($COL_ACC_PERMESSI) FROM $THIRD_TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        var sum = 0.0
        if (cursor.moveToFirst() && cursor.count > 0) {
            sum = cursor.getDouble(0)
        }
        cursor.close()
        return sum
    }

    fun deleteData(id: Int): Int {
        val db = this.writableDatabase
        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return db.delete(TABLE_NAME, whereClause, whereArgs)
    }

    fun deleteAccData(id: Int): Int {
        val db = this.writableDatabase
        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return db.delete(THIRD_TABLE_NAME, whereClause, whereArgs)
    }

    fun getFirstRow(): DataModel? {
        val db = readableDatabase
        val query = "SELECT * FROM $SECOND_TABLE_NAME LIMIT 1"
        val cursor = db.rawQuery(query, null)
        var firstRowData: DataModel? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndex(SECOND_COL_ID))
            val ferie = cursor.getDouble(cursor.getColumnIndex(COL_FERIE))
            val permessi = cursor.getDouble(cursor.getColumnIndex(COL_PERMESSI))
            val giorniFerie = cursor.getInt(cursor.getColumnIndex(COL_GIORNI_FERIE))
            val orePermessi = cursor.getDouble(cursor.getColumnIndex(COL_ORE_PERMESSI))
            val initialDate = cursor.getString(cursor.getColumnIndex(COL_INITIAL_DATE))
            firstRowData = DataModel(ferie, permessi, giorniFerie, orePermessi, initialDate, id)
        }
        cursor.close()
        return firstRowData
    }

    fun getDataByType(type: String): Cursor? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COL_TYPE = ?"
        val selectionArgs = arrayOf(type)

        return db.rawQuery(query, selectionArgs)
    }

    fun getAccumulatedDataByType(type: String): List<AccDataModel> {
        val db = readableDatabase
        val query = if (type == "Ferie") {
            "SELECT * FROM $THIRD_TABLE_NAME WHERE $COL_ACC_FERIE > 0.0"
        } else {
            "SELECT * FROM $THIRD_TABLE_NAME WHERE $COL_ACC_PERMESSI > 0.0"
        }

        val cursor = db.rawQuery(query, null)
        val dataList = mutableListOf<AccDataModel>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(THIRD_COL_ID))
                val accFerie = cursor.getDouble(cursor.getColumnIndex(COL_ACC_FERIE))
                val accPermessi = cursor.getDouble(cursor.getColumnIndex(COL_ACC_PERMESSI))
                val date = cursor.getString(cursor.getColumnIndex(COL_ACC_DATE))
                dataList.add(AccDataModel(accFerie, accPermessi, date, id))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return dataList
    }

    fun deleteAllData(): Int {
        val db = this.writableDatabase
        return db.delete(SECOND_TABLE_NAME, null, null)
    }

    fun deleteInsertedData(): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }

    fun deleteAllAccumulated(): Int {
        val db = this.writableDatabase
        return db.delete(THIRD_TABLE_NAME, null, null)
    }
}