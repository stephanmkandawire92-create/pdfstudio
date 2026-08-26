package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val pageCount: Int = 1,
    val fileSize: Long = 0,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false,
    val passwordHash: String? = null,
    val tags: String = "General",
    val thumbnailPath: String? = null,
    val ocrExtractedText: String = "",
    val isSample: Boolean = false
)

enum class AnnotationType {
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    DRAWING,
    TEXT_NOTE,
    FREE_TEXT,
    SIGNATURE,
    STAMP,
    SHAPE_RECT,
    SHAPE_CIRCLE,
    SHAPE_ARROW,
    SHAPE_LINE
}

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val type: AnnotationType,
    val colorHex: String = "#FFFFEB3B",
    val strokeWidth: Float = 4f,
    val opacity: Float = 0.5f,
    val pointsJson: String = "",       // Normalized path points (e.g. "x1,y1;x2,y2")
    val textContent: String = "",     // Text for notes, free text, stamps
    val rectBoundsJson: String = "",  // "left,top,right,bottom" normalized 0..1
    val extraData: String = "",       // Stamp style, shape info, font size
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val pointsJson: String,           // Vector stroke points
    val strokeColorHex: String = "#0F172A",
    val strokeWidth: Float = 4f,
    val isDefault: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "form_fields")
data class FormFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val fieldName: String,
    val fieldType: String,            // "TEXT", "CHECKBOX", "DATE", "SIGNATURE"
    val fieldValue: String,
    val rectBoundsJson: String
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC")
    suspend fun getAllDocumentsSync(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY lastOpenedTimestamp DESC")
    fun getFavoriteDocumentsFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE filePath = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("UPDATE documents SET lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE documentId = :docId AND pageIndex = :pageIndex ORDER BY id ASC")
    fun getAnnotationsForPageFlow(docId: Long, pageIndex: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE documentId = :docId ORDER BY pageIndex ASC, id ASC")
    suspend fun getAllAnnotationsForDoc(docId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE documentId = :docId AND pageIndex = :pageIndex")
    suspend fun getAnnotationsForPage(docId: Long, pageIndex: Int): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM annotations WHERE documentId = :docId")
    suspend fun deleteAllAnnotationsForDoc(docId: Long)
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY createdTimestamp DESC")
    fun getAllSignaturesFlow(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(sig: SignatureEntity): Long

    @Delete
    suspend fun deleteSignature(sig: SignatureEntity)

    @Query("UPDATE signatures SET isDefault = (id = :id)")
    suspend fun setDefaultSignature(id: Long)
}

@Dao
interface FormDao {
    @Query("SELECT * FROM form_fields WHERE documentId = :docId AND pageIndex = :pageIndex")
    fun getFormFieldsForPageFlow(docId: Long, pageIndex: Int): Flow<List<FormFieldEntity>>

    @Query("SELECT * FROM form_fields WHERE documentId = :docId")
    suspend fun getAllFormFieldsForDoc(docId: Long): List<FormFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateField(field: FormFieldEntity): Long

    @Query("UPDATE form_fields SET fieldValue = :value WHERE id = :id")
    suspend fun updateFieldValue(id: Long, value: String)
}

@Database(
    entities = [
        DocumentEntity::class,
        AnnotationEntity::class,
        SignatureEntity::class,
        FormFieldEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun signatureDao(): SignatureDao
    abstract fun formDao(): FormDao
}
