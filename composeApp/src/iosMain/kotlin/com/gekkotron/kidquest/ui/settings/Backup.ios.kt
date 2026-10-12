package com.gekkotron.kidquest.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.URLByAppendingPathComponent
import platform.Foundation.create
import platform.Foundation.temporaryDirectory
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import platform.UniformTypeIdentifiers.UTTypeJSON

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberBackupExport(): (filename: String, payload: String) -> Unit = remember {
    { filename, payload ->
        val dir = NSFileManager.defaultManager.temporaryDirectory
        dir.URLByAppendingPathComponent(filename)?.let { url ->
            val ns = NSString.create(string = payload)
            ns.writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
            presentFromTopmost(controller)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberBackupImport(onPicked: (text: String) -> Unit): () -> Unit {
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val accessed = url.startAccessingSecurityScopedResource()
                try {
                    val text = NSString.create(contentsOfURL = url, encoding = NSUTF8StringEncoding, error = null)
                    if (text != null) onPicked(text)
                } finally {
                    if (accessed) url.stopAccessingSecurityScopedResource()
                }
            }
        }
    }
    return remember(delegate) {
        {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON))
            picker.delegate = delegate
            presentFromTopmost(picker)
        }
    }
}

private fun presentFromTopmost(controller: platform.UIKit.UIViewController) {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    top?.presentViewController(controller, animated = true, completion = null)
}
