/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.publication

import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The POM elements Maven Central requires beside the coordinates: name, description, url, at least
 * one license, at least one developer and the scm url.
 */
internal object CentralPomRequirements {
    /** Names the required elements [pom] lacks or leaves blank, in the order Central lists them. */
    fun missing(pom: File): List<String> {
        val project = parse(pom)
        return buildList {
            if (project.childText("name").isNullOrBlank()) add("name")
            if (project.childText("description").isNullOrBlank()) add("description")
            if (project.childText("url").isNullOrBlank()) add("url")
            if (project.child("licenses")?.children("license").isNullOrEmpty()) add("licenses")
            if (project.child("developers")?.children("developer").isNullOrEmpty()) add("developers")
            if (project.child("scm")?.childText("url").isNullOrBlank()) add("scm")
        }
    }

    private fun parse(pom: File): Element {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        return factory.newDocumentBuilder().parse(pom).documentElement
    }

    private fun Element.children(name: String): List<Element> =
        (0 until childNodes.length)
            .map(childNodes::item)
            .filterIsInstance<Element>()
            .filter { (it.localName ?: it.tagName) == name }

    private fun Element.child(name: String): Element? = children(name).firstOrNull()

    private fun Element.childText(name: String): String? = child(name)?.textContent
}
