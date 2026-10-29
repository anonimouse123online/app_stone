package com.example.capstonesample.issues

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.IssueImage
import com.example.capstonesample.documents.projectDocumentUrl

@Composable
internal fun IssueImagesPreview(images: List<IssueImage>) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).okHttpClient(RetrofitClient.okHttpClient).build()
    }
    DisposableEffect(imageLoader) { onDispose { imageLoader.shutdown() } }
    Column {
        images.forEach { image ->
            val url = runCatching { projectDocumentUrl(image.url, RetrofitClient.BASE_URL).toString() }.getOrNull()
            if (url == null) {
                Text("Invalid issue image URL.", color = MaterialTheme.colorScheme.error)
            } else {
                AsyncImage(
                    model = url,
                    imageLoader = imageLoader,
                    contentDescription = image.filename,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
