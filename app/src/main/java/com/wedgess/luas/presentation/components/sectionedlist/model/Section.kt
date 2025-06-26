package com.wedgess.luas.presentation.components.sectionedlist.model

import kotlinx.collections.immutable.ImmutableList

class Section<T>(
    val id: Long,
    val title: String,
    val subTitle: String? = null,
    val items: ImmutableList<SectionItem<T>>,
)
