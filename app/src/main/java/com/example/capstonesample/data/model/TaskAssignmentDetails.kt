package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName

data class TaskAssignedResource(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("supplier") val supplier: String? = null
)

fun materialRequirementEntries(materialsRequired: String?): List<String> =
    materialsRequired.orEmpty()
        // Keep numeric commas (e.g. 1,000 bags) within the supplied description.
        .split(Regex("[;\\r\\n]+|(?<!\\d),|,(?!\\d)"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun taskAssignedResources(
    allocatedMaterials: List<TaskAssignedResource>?,
    resources: List<TaskAssignedResource>?,
    materialsRequired: String?
): List<TaskAssignedResource> {
    val assigned = resources.orEmpty()
    val materials = allocatedMaterials.orEmpty().ifEmpty {
        materialRequirementEntries(materialsRequired).map { TaskAssignedResource(name = it) }
    }
    fun sameItem(first: TaskAssignedResource, second: TaskAssignedResource): Boolean =
        first.name?.trim()?.takeIf { it.isNotEmpty() }
            ?.equals(second.name?.trim(), ignoreCase = true) == true

    return materials.map { material ->
        val resource = assigned.firstOrNull { sameItem(material, it) }
        material.copy(
            category = material.category ?: resource?.category,
            unit = material.unit ?: resource?.unit,
            supplier = material.supplier ?: resource?.supplier
        )
    } + assigned.filter { resource -> materials.none { sameItem(it, resource) } }
}

fun resourceQuantityLabel(resource: TaskAssignedResource): String? =
    listOfNotNull(
        resource.quantity?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() },
        resource.unit?.trim()?.takeIf { it.isNotEmpty() }
    ).joinToString(" ").takeIf { it.isNotEmpty() }
