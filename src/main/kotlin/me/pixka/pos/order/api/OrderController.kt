package me.pixka.pos.order.api

import jakarta.validation.Valid
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.service.OrderReceiptPdfService
import me.pixka.pos.order.service.OrderService
import me.pixka.pos.order.service.KitchenOrderPrintService
import me.pixka.pos.order.service.ReceiptTcpPrinterService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
    private val orderReceiptPdfService: OrderReceiptPdfService,
    private val receiptTcpPrinterService: ReceiptTcpPrinterService,
    private val kitchenOrderPrintService: KitchenOrderPrintService,
) {
    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<PosOrder> {
        return orderService.search(q)
    }

    @GetMapping("/report")
    fun report(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,
    ): OrderReport {
        return orderService.report(startDate, endDate)
    }

    /** Single order by id (includes whole-order `note`). Register after `/report` so the path is not captured as an id. */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): PosOrder {
        return orderService.getById(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: OrderRequest): PosOrder {
        return orderService.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: OrderRequest
    ): PosOrder {
        return orderService.update(id, request)
    }

    /** Update whole-order note only (works on paid orders; requires matching [PatchOrderNoteRequest.version]). */
    @PatchMapping("/{id}/note")
    fun patchOrderNote(
        @PathVariable id: Long,
        @Valid @RequestBody body: PatchOrderNoteRequest,
    ): PosOrder {
        return orderService.patchOrderNote(id, body)
    }

    @PostMapping("/{id}/pay")
    fun pay(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) body: PayOrderRequest?,
    ): PosOrder {
        return orderService.pay(id, body)
    }

    /**
     * Confirms payment after scanning a customer / payment QR. Same settlement rules as `POST …/pay`;
     * always records [PayOrderRequest.paidByQrScan] and the scanned reference string.
     */
    @PostMapping("/{id}/pay/qr-scan")
    fun payByQrScan(
        @PathVariable id: Long,
        @Valid @RequestBody body: PayByQrScanRequest,
    ): PosOrder {
        return orderService.pay(
            id,
            PayOrderRequest(
                paidPrice = body.paidPrice,
                change = body.change,
                paidByQrScan = true,
                qrScanPayload = body.qrScanPayload,
            ),
        )
    }

    @GetMapping("/{id}/receipt")
    fun receipt(@PathVariable id: Long): OrderReceipt {
        return orderService.receipt(id)
    }

    @GetMapping("/{id}/receipt.pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun receiptPdf(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val bytes = orderReceiptPdfService.render(orderService.receipt(id))
        val filename = "receipt-${id}.pdf"
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
            .body(bytes)
    }

    @PostMapping("/{id}/receipt/print")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun printReceipt(@PathVariable id: Long) {
        receiptTcpPrinterService.printOrderReceipt(id)
    }

    /** Prints kitchen tickets (food name + qty) to each kitchen's TCP printer from the database. */
    @PostMapping("/{id}/print-kitchens", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun printKitchens(@PathVariable id: Long): KitchenPrintResponse {
        return kitchenOrderPrintService.printOrderToKitchens(id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        orderService.delete(id)
    }
}
