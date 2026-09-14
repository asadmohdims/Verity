package com.verity.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.core.document.model.InvoiceDocumentModel
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * DocumentDetailViewModel
 *
 * Screen-level controller for the Document Detail screen — reopening a previously finalized
 * document (from Home's Recent Documents or the Documents tab), as opposed to
 * InvoiceWorkspaceViewModel's in-memory finalizedDocument, which only ever holds the document
 * just finalized in this session.
 *
 * Scoped to one documentId per instance (constructed per nav back-stack entry via
 * viewModelFactory in AppNavShell) — this app's first argument-scoped ViewModel.
 */
class DocumentDetailViewModel(
    documentId: String,
    dataSource: DocumentDetailDataSource,
    private val invoicePdfRenderer: InvoicePdfRenderer
) : ViewModel() {

    private val _document = MutableStateFlow<InvoiceDocumentModel?>(null)
    val document: StateFlow<InvoiceDocumentModel?> = _document.asStateFlow()

    init {
        viewModelScope.launch {
            _document.value = dataSource.loadDocument(documentId)
        }
    }

    /** Safe to call every time the PDF viewer opens - ensurePdf() is idempotent. */
    suspend fun ensurePdf(): File {
        val document = requireNotNull(document.value) {
            "Document not loaded yet"
        }
        return invoicePdfRenderer.ensurePdf(document)
    }
}
