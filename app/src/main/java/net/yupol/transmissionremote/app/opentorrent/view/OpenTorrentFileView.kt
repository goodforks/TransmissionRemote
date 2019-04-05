package net.yupol.transmissionremote.app.opentorrent.view

import com.hannesdorfmann.mosby3.mvp.MvpView
import net.yupol.transmissionremote.model.Dir
import java.util.*

interface OpenTorrentFileView: MvpView {

    fun showDir(dir: Dir)

    fun showBreadcrumbs(path: Stack<Dir>)

    fun updateFileList()

    fun getDownloadDirectory(): String

    fun isTrashTorrentFileChecked(): Boolean

    fun isStartWhenAddedChecked(): Boolean
}
