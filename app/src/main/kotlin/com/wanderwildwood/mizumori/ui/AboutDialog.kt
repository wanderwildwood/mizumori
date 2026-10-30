package com.wanderwildwood.mizumori.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.mizumori.BuildConfig
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.mizumori.R

/**
 * What it is, what it cannot promise, and whose work it continues.
 *
 * The line about accuracy is the important one. A phone's accelerometer is not a
 * machinist's level, and an app that shows a tenth of a degree looks far more certain
 * than the hardware behind it is.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        // The words scroll and the Close button stays put, so a short screen (the phone on its
        // side) never pushes it out of reach.
        PagedColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            item {
                TextMMD(
                    text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            item { Heading(stringResource(R.string.about_privacy_heading)) }
            item { TextMMD(text = stringResource(R.string.about_privacy), style = MaterialTheme.typography.labelSmall) }
            item { TextMMD(text = stringResource(R.string.about_accuracy), style = MaterialTheme.typography.labelSmall) }
            item { TextMMD(text = stringResource(R.string.about_built_on), style = MaterialTheme.typography.labelSmall) }
            item { Heading(stringResource(R.string.about_terms_heading)) }
            item { TextMMD(text = stringResource(R.string.about_terms), style = MaterialTheme.typography.labelSmall) }
            item { TextMMD(text = stringResource(R.string.about_licence), style = MaterialTheme.typography.labelSmall) }
            item { TextMMD(text = stringResource(R.string.about_icons), style = MaterialTheme.typography.labelSmall) }
            item { LicencesLink() }
            item { Llama() }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.about_close), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Heading(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 * The site's address sits at the start of the same line and opens the site; the llama and its words
 * open the page.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.labelSmall,
            // The site's address opens the site, the way the llama beside it opens its page.
            modifier = Modifier
                .clickable {
                    openLink(context, "https://wanderthe.dev", context.getString(R.string.about_no_browser_address, "wanderthe.dev"))
                }
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    // Straight to the checkout. The Donate button on the site only leads
                    // here anyway, so the page in between is a press the reader does not need.
                    // The short square.link form, not the long checkout.square.site address it
                    // redirects to -- the short one is what the site itself links to, so a
                    // regenerated checkout follows it and a published app does not break.
                    openLink(context, "https://square.link/u/AGu8oT10", context.getString(R.string.about_no_browser_address, "square.link/u/AGu8oT10"))
                }
                .heightIn(min = 48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.labelSmall)
        }
    }
}
