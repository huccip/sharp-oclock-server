package com.example.sharpoclockserver.product

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.*

// GET http://localhost:8080/admin/watches?id=123
// GET http://localhost:8080/admin/watches/create
// GET http://localhost:8080/admin/watches/update?id=123
// GET http://localhost:8080/admin/watches/delete?id=123
// GET http://localhost:8080/admin/watches/generate-details?prompt=bla_bla

@Controller
@RequestMapping("/admin/watches")
class AdminWatchController(
    val watchService: WatchService
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
        var sizes: List<String>,
        @NotNull
        @Min(40)
        @Max(1_000_000)
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve

        var isOOS: Boolean? = null, // Out Of Stock
        var isDiscounted: Boolean? = null,
        var isNew: Boolean? = null,

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
        var sizes: List<String>,
        @NotNull
        @Min(40)
        @Max(1_000_000)
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve

        var isOOS: Boolean?, // Out Of Stock
        var isDiscounted: Boolean?,
        var isNew: Boolean?,

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
        var sizes: List<String>,
        var priceDisplay: BigDecimal, // multiply by 100 to store and divide by 100 to retrieve
        val discountedPriceDisplay: BigDecimal?,
        val unitsSold: Long,

        var isOOS: Boolean? = null, // Out Of Stock
        var isDiscounted: Boolean? = null,
        var isNew: Boolean? = null,
        val isPopular: Boolean?,

        var discountPercentage: Int? = null,
    )

    @GetMapping("/create")
    fun createWatchTemplate() = "admin/watch/create"

    @PostMapping("/save")
    fun saveWatch(
        @ModelAttribute @Valid saveWatchRequest: SaveWatchRequest
    ): String {
        watchService.saveWatch(saveWatchRequest)
        return "redirect:/admin/watches/list"
    }

    @ResponseBody
    @GetMapping("/generate-details")
    fun generateDetails(
        @RequestParam prompt: String,
    ) = watchService.generateDetails(prompt)

    @GetMapping("/list")
    fun listWatchesTemplate(
        model: Model
    ): String {
        model.addAttribute(
            "watches",
            watchService.listWatches()
        )
        return "admin/watch/list"
    }

    @PostMapping("/delete/{watchId}")
    fun deleteWatch(
        @PathVariable watchId: UUID,
    ): String {
        watchService.deleteWatchById(watchId)
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
            watchService.getWatchForEdit(watchId)
        )

        return "admin/watch/edit"
    }

    @PostMapping("/update/{watchId}")
    fun updateWatch(
        @PathVariable watchId: UUID,
        @ModelAttribute @Valid editWatchRequest: SaveWatchRequest,
    ): String {
        watchService.updateWatchById(watchId, editWatchRequest)
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
            watchService.getWatchForDetails(watchId)
        )

        return "admin/watch/details"
    }
}
