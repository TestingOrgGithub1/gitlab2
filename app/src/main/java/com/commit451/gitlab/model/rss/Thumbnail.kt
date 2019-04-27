package com.commit451.gitlab.model.rss

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root

@Root(strict = false)
class Thumbnail {
    @field:Attribute(name = "url", required = true)
    lateinit var url: String
}
