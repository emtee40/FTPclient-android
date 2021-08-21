package de.qwerty287.ftpclient.ui.files

import android.graphics.drawable.Drawable
import de.qwerty287.ftpclient.R
import org.apache.commons.net.ftp.FTPFile

object FileExtensions {
    // Lists of file extensions. Extracted from a Linux system using the Python scripts in <project root>/scripts.
    private val audioFormats = listOf("aac", "adts", "ass", "mka", "minipsf", "it", "oga", "ogg", "opus", "xm", "s3m", "axa", "psf", "pls", "mo3", "sid", "psid", "aax", "psflib", "spx", "mpc", "mpp", "mp+", "opus", "wvc",
        "m4b", "f4b", "oga", "ogg", "spx", "xi", "aifc", "aiffc", "mp3", "mpga", "au", "snd", "wma", "gsm", "wv", "wvp", "m3u", "m3u8", "vlc", "awb", "ape", "dts", "oga", "ogg", "voc", "mod", "ult", "uni", "m15", "mtm",
        "669", "med", "mid", "midi", "kar", "asx", "wax", "wvx", "wmx", "wav", "flac", "ac3", "mp2", "aiff", "aif", "dtshd", "loas", "xhe", "aa", "m4a", "f4a", "xmf", "amr", "m4r", "amz", "stm", "pla", "tta", "ra", "rax",
        "oga", "ogg",
        // extensions below this comment are extracted from application mime types
        "shn", "xspf", "mmf", "smaf")
    private val videoFormats = listOf("viv", "vivo", "mjpeg", "mjpg", "mkv", "ogv", "ogg", "fxm", "axv", "mlt", "westley", "anim[1-9j]", "dv", "wmp", "webm", "nsv", "ogm", "mpeg", "mpg", "mp2", "mpe", "vob", "vdr", "flv",
        "qt", "mov", "moov", "qtvr", "3gp", "3gpp", "3ga", "3g2", "3gp2", "3gpp2", "avi", "avf", "divx", "mj2", "mjp2", "ogg", "movie", "rv", "rvx", "m1u", "m4u", "mxu", "fli", "flc", "mp4", "m4v", "f4v", "lrv", "m2t",
        "m2ts", "ts", "mts", "cpi", "clpi", "mpl", "mpls", "bdm", "bdmv", "wmv", "mk3d", "mng",
        // extensions below this comment are extracted from application mime types
        "nsc", "sdp", "ogx", "anx", "swf", "spl", "mxf", "wpl", "smil", "smi", "sml", "kino", "qtl", "rm", "rmj", "rmm", "rms", "rmx", "rmvb", "yt")
    private val imageFormats = listOf("pic", "psd", "xcf", "fig", "bay", "bmq", "cs1", "cs2", "erf", "fff", "hrd", "mdc", "mos", "pnx", "rdc", "gif", "ief",
        "bmp", "dib", "nef", "emf", "orf", "wmf", "lws", "sgi", "avifs", "ppm", "k25", "xwd", "rgb", "j2c", "j2k", "jpc", "hdr", "pic", "dxf", "heic", "heif", "xbm", "cgm", "ag", "pat", "rle", "svgz",
        "ora", "tif", "tiff", "ico", "jpm", "jpgm", "ktx", "exr", "dcr", "rp", "dwg", "pcd", "dng", "gbr", "mrw", "png", "sr2", "crw", "eps", "epsi", "epsf", "jng", "gih", "fits", "lwo", "lwob", "rw2", "srf", "iff", "ilbm",
        "lbm", "jpg", "jpeg", "jpe", "3ds", "pef", "jpf", "jpx", "raf", "dds", "mdi", "pgm", "djvu", "djv", "sun", "cur", "avif", "g3", "raw", "webp", "xpm", "svg", "wbmp", "pct", "pict", "pict1", "pict2", "qtif", "qif",
        "kdc", "msod", "cr2", "x3f", "sk", "sk1", "ras", "pbm", "tga", "icb", "tpic", "vda", "vst", "pntg", "jp2", "jpg2", "pcx", "djvu", "djv", "icns", "pnm", "arw",
        // extensions below this comment are extracted from application mime types
        "otg", "otg", "karbon", "fodg", "fodg", "kpm", "vsdm", "shape", "vssx", "odg", "odg", "vstm", "dia", "dcm", "std", "std", "sxd", "sxd", "cdr", "ai", "kil", "kon", "egon", "vstx", "pcl", "odi", "blender", "blend",
        "sda", "vsdx", "hpgl", "wpg", "vssm", "ufraw")
    private val fontFormats = listOf("woff2", "ttc", "ttf", "otf", "woff",
        // extensions below this comment are extracted from application mime types
        "afm", "gf", "ttx", "spd", "bdf", "psf", "pcf", "pk", "pfa", "pfb", "gsf")
    private val spreadsheetFormats = listOf(
        // extensions below this comment are extracted from application mime types
        "siag", "pys", "qif", "otc", "ksp", "xls", "xlc", "xll", "xlm", "xlw", "xla", "xlt", "xld", "xls", "xltm", "xltm", "chrt", "xlam", "pln", "ods", "ods", "xltx", "xltx", "sds", "xlsm", "xlsm", "stc", "stc", "ots",
        "ots", "gnumeric", "pysu", "fods", "fods", "odc", "xlsb", "xlsb", "sdc", "xlsx", "xlsx", "123", "wk1", "wk3", "wk4", "wks", "oleo", "gnucash", "gnc", "xac", "as", "wb1", "wb2", "wb3", "sxc", "sxc")
    private val locationFormats = listOf(
        // extensions below this comment are extracted from application mime types
        "shp", "osm", "osc", "gpx")
    private val textFormats = listOf(
        // extensions below this comment are extracted from application mime types
        "o", "stw", "stw", "sxm", "sxm", "abw", "zabw", "cbt", "kra", "cbz", "rtf", "dot", "mobi", "prc", "fodt", "fodt", "dotm", "dotm", "oda", "sdw", "vor", "sgl", "vsd", "vst", "vsw", "vss", "fl",
        "cb7", "ui", "fm", "kfo", "ez", "pdf", "wwf", "dbk", "docbook", "kud", "sxg", "sxg", "wcm", "wdb", "wks", "wps", "xlr", "sam", "wp", "wp4", "wp5", "wp6", "wpd",
        "wpp", "sxw", "sxw", "wri", "ott", "ott", "odt", "odt", "otf", "odb", "odf", "odf", "xul", "nb", "jpr", "jpx", "lyx", "glade", "pdb", "pdc", "epub", "p65", "pm", "pm6", "pmd", "cwk", "smf", "oxt", "oxt",
        "aw", "ged", "gedcom", "dbf", "dvi", "odm", "odm", "pw", "ui", "obj", "doc", "doc", "lwp", "ps", "chm", "docx", "docx", "cdf", "nc", "gra", "docm", "docm", "hdf", "hdf4", "h4", "hdf5", "h5", "mdb", "hwt", "qti",
        "gp", "gplt", "gnuplot", "dotx", "dotx", "602", "ipynb", "oxps", "xps", "kwd", "kwt", "cbr", "psw", "hwp", "flw", "sgf", "skr", "pkr", "asc", "pgp", "gpg", "key", "it87", "desktop",
        "kdelnk", "srt", "der", "crt", "cert", "pem", "p10", "p10", "csr", "xsl", "xslt", "src", "smi", "sami", "mbox", "p7c", "p7m", "p7c", "p7m", "spc", "p7b", "xlf", "xliff", "man", "out", "p7s", "p7s", "toc", "rnc",
        "pgn", "cue", "asc", "sig", "pgp", "gpg", "ccmx", "ica", "pgp", "gpg", "asc", "pcf", "dtd", "yaml", "yml", "ent", "oth", "oth", "xhtml", "xht", "html", "htm", "xbel", "atom", "rss", "xml", "xbl", "xsd", "rng", "js",
        "jsm", "mjs", "jrd", "jnlp", "la", "asp", "csh", "m4", "mab", "json", "rb", "awk", "siv", "pl", "PL", "pm", "al", "perl", "pod", "t", "php", "php3", "php4", "php5", "phps", "sh", "es", "coffee", "json-patch",
        "jsonld", "kcfgc", "notifyrc", "kcfg", "rc")
    private val packageFormats = listOf(
        // extensions below this comment are extracted from application mime types
        "shar", "flatpakrepo", "emp", "tlrz", "spm", "zz", "flatpakref", "sv4cpio", "zst", "rpm", "taz", "lzo", "jar", "tzo", "wad", "lz4", "gz", "arj", "Z", "pack",
        "zip", "bcpio", "lhz", "wkdownload", "crdownload", "part", "PAR2", "par2", "lrz", "pak", "cab", "theme", "rar", "sis", "tbz2", "tbz", "tb2", "7z", "deb", "udeb", "qp", "lz", "alz",
        "tlz", "lha", "lzh", "sit", "bz2", "bz", "flatpak", "xdgapp", "ustar", "lzma", "tgz", "xz", "dar", "zoo", "ace", "tar", "gtar", "gem", "sv4crc", "themepack", "tzst", "a",
        "ar", "txz", "xar", "pkg", "cpio")
    private val presentationFormats = listOf(
        // extensions below this comment are extracted from application mime types
        "sldx", "kpr", "kpt", "ppsx", "otp", "otp", "potx", "potx", "sldm", "potm", "potm", "key", "ppam", "odp", "odp", "sdd", "sdp", "fodp", "fodp", "sxi", "sxi", "ppz", "ppt", "pps", "pot", "ppt", "pptx", "pptx", "pptm",
        "pptm", "ppsm", "sti", "sti", "mgp")

    /**
     * Returns the [Drawable] resource for the specific file
     * @param file The [FTPFile]
     * @return The [Drawable] resource
     */
    fun getDrawableFromFTPFile(file: FTPFile): Int {
        return if (file.isDirectory) {
            R.drawable.ic_baseline_folder_24
        } else if (file.isFile && getFileFormat(file) in audioFormats) {
            R.drawable.ic_baseline_music_note_24
        } else if (file.isFile && getFileFormat(file) in videoFormats) {
            R.drawable.ic_baseline_local_movies_24
        } else if (file.isFile && getFileFormat(file) in imageFormats) {
            R.drawable.ic_baseline_image_24
        } else if (file.isFile && getFileFormat(file) in fontFormats) {
            R.drawable.ic_baseline_font_download_24
        } else if (file.isFile && getFileFormat(file) in spreadsheetFormats) {
            R.drawable.ic_baseline_table_chart_24
        } else if (file.isFile && getFileFormat(file) in locationFormats) {
            R.drawable.ic_baseline_location_on_24
        } else if (file.isFile && getFileFormat(file) in textFormats) {
            R.drawable.ic_baseline_text_snippet_24
        } else if (file.isFile && getFileFormat(file) in packageFormats) {
            R.drawable.ic_baseline_archive_24
        } else if (file.isFile && getFileFormat(file) in presentationFormats) {
            R.drawable.ic_baseline_slideshow_24
        } else if (file.isFile) {
            R.drawable.ic_baseline_insert_drive_file_24
        } else {
            R.drawable.ic_baseline_error_24
        }
    }

    /**
     * Returns the file format of an [FTPFile]
     * @param file The [FTPFile]
     * @return The [String] with the end of the file name
     */
    private fun getFileFormat(file: FTPFile): String {
        val list = file.name.split(".")
        return if (list.isEmpty()) {
            ""
        } else {
            list.last().lowercase()
        }
    }
}