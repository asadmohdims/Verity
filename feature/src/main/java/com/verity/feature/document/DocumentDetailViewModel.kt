package com.verity.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verity.core.document.model.DocumentType
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

    /**
     * The job-work-linked document's id, if this document has a jobWorkLink AND that link
     * resolves to a real row — null otherwise (no link, or a job-work Challan whose reserved
     * Invoice was never finalized). Resolved differently per side of the link: an Invoice
     * already carries its Challan's real id directly on jobWorkLink (see DefaultInvoiceFinalizer);
     * a Challan doesn't (that id didn't exist yet at its own finalize time), so it needs the
     * reverse lookup instead.
     */
    private val _linkedDocumentId = MutableStateFlow<String?>(null)
    val linkedDocumentId: StateFlow<String?> = _linkedDocumentId.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = dataSource.loadDocument(documentId)
            _document.value = loaded

            val jobWorkLink = loaded?.jobWorkLink
            _linkedDocumentId.value = when {
                jobWorkLink == null -> null
                loaded.identity.documentType == DocumentType.INVOICE -> jobWorkLink.linkedDocumentId
                else -> dataSource.findLinkedDocumentId(documentId)
            }
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
