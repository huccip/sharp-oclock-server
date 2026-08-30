package com.example.sharpoclockserver.product

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.*

// GET http://localhost:8080/admin/watches/generate-details?prompt=bla_bla

// GET http://localhost:8080/admin/watches/details/{watchId}: details.html template

// GET http://localhost:8080/admin/watches/create: create.html template
// POST http://localhost:8080/admin/watches/save: saves then embeds in pg-vector store

// GET http://localhost:8080/admin/watches/edit/{watchId}: edit.html template
// POST http://localhost:8080/admin/watches/update/{watchId}: updates then re-embeds in pg-vector store

// GET http://localhost:8080/admin/watches/delete/{watchId}: deletes then deletes its embedding in pg-vector store

// GET http://localhost:8080/admin/watches/list: list.html template

@Controller
@RequestMapping("/admin/watches")
class AdminWatchController(
    val adminWatchService: AdminWatchService
) {

    data class WatchDetails(
        val brand: String,
        val name: String,
        val description: String,
    )

    data class SaveWatchRequest(
        var thumbnailImage: MultipartFile? = null,
        var heroImage: MultipartFile? = null,

        @NotNull
        @NotBlank
        var brand: String,
        @NotNull
        @NotBlank
        var name: String,
        @NotNull
        @NotBlank
        var description: String,
        @NotNull
        @NotEmpty
        var sizes: List<Int>,
        @NotNull
        @Min(40)
        @Max(1_000_000)
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve

        var isOOS: Boolean = false, // Out Of Stock
        var isDiscounted: Boolean = false,
        var isNew: Boolean = true,

        @Min(1)
        @Max(100)
        var discountPercentage: Int? = null,
    )

    data class EditWatchView(
        val thumbnailUrl: String,
        val heroUrl: String,

        @NotNull
        @NotBlank
        var brand: String,
        @NotNull
        @NotBlank
        var name: String,
        @NotNull
        @NotBlank
        var description: String,
        @NotNull
        @NotEmpty
        var sizes: List<Int>,
        @NotNull
        @Min(40)
        @Max(1_000_000)
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve

        var isOOS: Boolean, // Out Of Stock
        var isDiscounted: Boolean,
        var isNew: Boolean,

        @Min(1)
        @Max(100)
        var discountPercentage: Int?,
    )

    data class WatchDetailsView(
        val thumbnailUrl: String,
        val heroUrl: String,

        var brand: String,
        var name: String,
        var description: String,
        var sizes: List<Int>,
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve
        val discountedPriceDisplay: BigDecimal?,
        val unitsSold: Long,

        var isOOS: Boolean, // Out Of Stock
        var isDiscounted: Boolean,
        var isNew: Boolean,
        val isPopular: Boolean,

        var discountPercentage: Int? = null,
    )

    @GetMapping("/create")
    fun createWatchTemplate() = "admin/watch/create"

    @PostMapping("/save")
    fun saveWatch(
        @ModelAttribute @Valid saveWatchRequest: SaveWatchRequest
    ): String {
        adminWatchService.saveWatch(saveWatchRequest)
        return "redirect:/admin/watches/list"
    }

    @ResponseBody
    @GetMapping("/generate-details")
    fun generateDetails(
        @RequestParam prompt: String,
    ) = adminWatchService.generateDetails(prompt)

    @GetMapping("/list")
    fun listWatchesTemplate(
        model: Model
    ): String {
        model.addAttribute(
            "watches",
            adminWatchService.listWatches()
        )
        return "admin/watch/list"
    }

    @PostMapping("/delete/{watchId}")
    fun deleteWatch(
        @PathVariable watchId: UUID,
    ): String {
        adminWatchService.deleteWatchById(watchId)
        return "redirect:/admin/watches/list"
    }

    @GetMapping("/edit/{watchId}")
    fun editWatchTemplate(
        @PathVariable watchId: UUID,
        model: Model,
    ): String {
        model.addAttribute("watchId", watchId)
        model.addAttribute(
            "watch",
            adminWatchService.getWatchForEdit(watchId)
        )

        return "admin/watch/edit"
    }

    @PostMapping("/update/{watchId}")
    fun updateWatch(
        @PathVariable watchId: UUID,
        @ModelAttribute @Valid editWatchRequest: SaveWatchRequest,
    ): String {
        adminWatchService.updateWatchById(watchId, editWatchRequest)
        return "redirect:/admin/watches/list"
    }

    @GetMapping("/details/{watchId}")
    fun detailsTemplate(
        @PathVariable watchId: UUID,
        model: Model,
    ): String {
        model.addAttribute("watchId", watchId)
        model.addAttribute(
            "watch",
            adminWatchService.getWatchForDetails(watchId)
        )

        return "admin/watch/details"
    }
}
