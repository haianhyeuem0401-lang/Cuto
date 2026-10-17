package com.qbao.tweak

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : Activity() {
    private val apiBase = "https://qbaonew1.onrender.com"
    private val bg = Color.rgb(7, 17, 13)
    private val panel = Color.rgb(17, 32, 25)
    private val green = Color.rgb(0, 230, 118)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 190, 179)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var enteredKey = ""
    private var expiresAt: String? = null
    private var currentTab = 0
    private var video: VideoView? = null
    private var root: LinearLayout? = null
    private var content: LinearLayout? = null
    private var runButton: Button? = null
    private var runProgress: TextView? = null
    private var runTask = 0
    private val aimNames = listOf("🌈 LightAim", "🎯 AimFix", "⚡ AntiShake", "💎 SoftAim",
        "🔥 RecoilFix", "👑 AimFlow", "🌀 SmoothTouch", "💥 AimBoost",
        "🧿 StableAim", "🚀 UltraSmooth")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showLogin()
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 16f, color: Int = white, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }
    }
    private fun button(value: String, action: () -> Unit): Button = Button(this).apply {
        text = value
        setTextColor(Color.BLACK)
        setBackgroundColor(green)
        setOnClickListener { action() }
        isAllCaps = false
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundColor(panel)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(6), 0, dp(6))
        layoutParams = lp
    }
    private fun scroll(child: View): ScrollView = ScrollView(this).apply {
        addView(child)
        isFillViewport = true
    }

    private fun showLogin() {
        val frame = FrameLayout(this).apply { setBackgroundColor(bg) }
        video = VideoView(this)
        frame.addView(video, FrameLayout.LayoutParams(-1, -1))
        try {
            val uri = Uri.parse("android.resource://${packageName}/${R.raw.login_video}")
            video?.setVideoURI(uri)
            video?.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(1f, 1f)
                video?.start()
            }
        } catch (_: Exception) {}
        val shade = View(this).apply { setBackgroundColor(0xB9000000.toInt()) }
        frame.addView(shade, FrameLayout.LayoutParams(-1, -1))
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        form.addView(text("⚡ QBaoTweakxVft", 28f, green, true))
        form.addView(text("Nhập key để xác thực", 16f, white))
        val input = EditText(this).apply {
            hint = "QBAO-XXXX-XXXX-XXXX"
            setHintTextColor(muted); setTextColor(white)
            setSingleLine(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(0xCC182820.toInt())
        }
        form.addView(input, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
        val status = text("", 14f, green)
        form.addView(button("Kiểm tra key") {
            val key = input.text.toString().trim()
            if (key.isBlank()) { status.text = "Vui lòng nhập key."; status.setTextColor(Color.RED); return@button }
            status.text = "Đang kết nối máy chủ..."
            status.setTextColor(green)
            verifyKey(key) { ok, message, expiry ->
                if (ok) {
                    enteredKey = key
                    expiresAt = expiry
                    showHome()
                } else {
                    status.text = "❌ $message"
                    status.setTextColor(Color.RED)
                }
            }
        }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) } })
        form.addView(status)
        form.addView(text("Key được kiểm tra trực tiếp qua máy chủ Qbaonew.", 12f, muted))
        frame.addView(form, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    }

    private fun verifyKey(key: String, done: (Boolean, String, String?) -> Unit) {
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL("$apiBase/api/check-key").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 12000; readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = JSONObject().put("key", key).toString()
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                val valid = obj.optBoolean("valid", false)
                val msg = obj.optString("message", "Không xác định được trạng thái key.")
                val expiry = if (obj.isNull("expiresAt")) null else obj.optString("expiresAt", null)
                runOnUiThread { done(valid, msg, expiry) }
            } catch (_: Exception) {
                runOnUiThread { done(false, "Không kết nối được máy chủ. Thử lại sau.", null) }
            } finally { conn?.disconnect() }
        }
    }

    private fun showHome() {
        video?.stopPlayback()
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(12), dp(8), dp(12), dp(4)) }
        val header = card()
        header.addView(text("⚡ QBaoTweakxVft", 22f, green, true))
        header.addView(text("Key: ${maskKey(enteredKey)}", 14f, white))
        header.addView(text("Thời hạn: ${remainingText(expiresAt)}", 13f, muted))
        root!!.addView(header)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        root!!.addView(content)
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4)) }
        listOf("🎯 Aim", "🛠 Tối ưu", "👑 Admin").forEachIndexed { index, label ->
            val b = Button(this).apply {
                text = label; isAllCaps = false; textSize = 12f
                setTextColor(if (currentTab == index) green else white)
                setBackgroundColor(panel)
                setOnClickListener { currentTab = index; renderTab() }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        root!!.addView(nav)
        setContentView(root)
        renderTab()
    }

    private fun maskKey(key: String): String =
        if (key.length <= 10) key else key.take(5) + "••••••" + key.takeLast(4)

    private fun remainingText(expiry: String?): String {
        if (expiry.isNullOrBlank() || expiry == "null") return "Vĩnh viễn"
        return try {
            val end = java.time.Instant.parse(expiry).toEpochMilli()
            val ms = end - System.currentTimeMillis()
            if (ms <= 0) "Đã hết hạn" else {
                val hours = max(0L, ms / 3600000L)
                "${hours / 24} ngày ${hours % 24} giờ"
            }
        } catch (_: Exception) { "Đã kết nối máy chủ" }
    }

    private fun renderTab() {
        val c = content ?: return
        c.removeAllViews()
        when (currentTab) {
            0 -> renderAim(c)
            1 -> renderOptimize(c)
            else -> renderAdmin(c)
        }
    }

    private fun renderAim(c: LinearLayout) {
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(8)) }
        inner.addView(text("Hỗ trợ kéo tâm", 20f, green, true))
        inner.addView(animeBanner())
        inner.addView(text("Các công tắc dưới đây là tùy chọn giao diện; không chỉnh sửa bộ nhớ hay can thiệp cơ chế Free Fire.", 12f, muted))
        aimNames.forEach { name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(2), dp(8), dp(2)); setBackgroundColor(panel)
            }
            row.addView(text(name, 15f, white, true), LinearLayout.LayoutParams(0, dp(48), 1f))
            row.addView(Switch(this).apply {
                text = ""; buttonTintList = android.content.res.ColorStateList.valueOf(green)
            })
            inner.addView(row, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(5) })
        }
        runProgress = text("Chọn chức năng rồi nhấn Chạy.", 13f, green)
        inner.addView(runProgress)
        runButton = button("▶ Chạy") { startRunSequence() }
        inner.addView(runButton, LinearLayout.LayoutParams(-1, dp(48)))
        c.addView(scroll(inner), LinearLayout.LayoutParams(-1, -1))
    }

    private fun startRunSequence() {
        val btn = runButton ?: return
        btn.isEnabled = false
        var second = 0
        val messages = listOf("Đang kiểm tra cấu hình...", "Đang áp dụng thiết lập giao diện...",
            "Đang kiểm tra trạng thái thiết bị...", "Đang hoàn tất...")
        val task = ++runTask
        val tick = object : Runnable {
            override fun run() {
                if (task != runTask) return
                val view = runProgress
                if (second >= 30) {
                    view?.text = "✅ Hoàn tất. Chọn phiên bản Free Fire."
                    btn.isEnabled = true
                    showGameChooser()
                    return
                }
                view?.text = "🟢 ${messages[(second / 7) % messages.size]} ${second + 1}/30s"
                second++
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)
    }

    private fun showGameChooser() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(12)); setBackgroundColor(panel) }
        box.addView(text("Chọn phiên bản game", 19f, green, true))
        box.addView(button("Free Fire thường") { launchGame("com.dts.freefireth") })
        box.addView(button("Free Fire MAX") { launchGame("com.dts.freefiremax") })
        box.addView(button("Để sau") { (box.parent as? ViewGroup)?.removeView(box) })
        val dialog = android.app.AlertDialog.Builder(this).setView(box).create()
        dialog.show()
    }

    private fun launchGame(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
            Toast.makeText(this, "Mời bạn vào Game!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Chưa tìm thấy game này trên thiết bị.", Toast.LENGTH_LONG).show()
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) } catch (_: Exception) {}
        }
    }

    private fun renderOptimize(c: LinearLayout) {
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(8)) }
        inner.addView(text("🛠 Tối ưu thiết bị", 20f, green, true))
        inner.addView(animeBanner())
        inner.addView(text("Không có cách nào bảo đảm tối ưu 100% trên mọi máy. Các gợi ý an toàn:", 13f, muted))
        listOf(
            "✓ Bật chế độ hiệu năng/game mode nếu máy có hỗ trợ",
            "✓ Đóng ứng dụng nền không cần thiết",
            "✓ Giữ trống bộ nhớ lưu trữ và cập nhật hệ điều hành",
            "✓ Tắt tiết kiệm pin khi chơi nếu cần hiệu năng",
            "✓ Dùng DPI và độ nhạy phù hợp; ghi lại thiết lập cũ trước khi đổi",
            "✓ Không xóa file hệ thống hoặc file game để tránh lỗi/khóa tài khoản"
        ).forEach { inner.addView(card().apply { addView(text(it, 14f, white)) }) }
        inner.addView(button("Mở cài đặt thiết bị") {
            try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
        })
        c.addView(scroll(inner), LinearLayout.LayoutParams(-1, -1))
    }

    private fun renderAdmin(c: LinearLayout) {
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(8)) }
        inner.addView(text("👑 Admin", 20f, green, true))
        inner.addView(animeBanner())
        inner.addView(card().apply {
            addView(text("QBaoTweakxVft King", 18f, white, true))
            addView(text("Phạm Gia Bảo", 16f, green, true))
            addView(text("Buy key, DPI và các hỗ trợ khác: liên hệ admin.", 14f, muted))
            addView(button("🌐 Mở trang check key") { openUrl(apiBase) })
        })
        inner.addView(text("Key đang dùng: $enteredKey", 12f, muted))
        c.addView(scroll(inner), LinearLayout.LayoutParams(-1, -1))
    }

    private fun animeBanner(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xCC17251F.toInt())
        }
        box.addView(text("🌌 ANIME WALLPAPER THEME", 13f, green, true))
        box.addView(text("Hình nền anime từ WallNest HD • 4K", 14f, white, true))
        box.addView(text("Nhấn để xem bộ sưu tập và chọn ảnh phù hợp.", 12f, muted))
        box.setOnClickListener { openUrl("https://wallnesthd.com/anime-wallpapers.html") }
        box.layoutParams = LinearLayout.LayoutParams(-1, dp(86)).apply {
            topMargin = dp(4); bottomMargin = dp(8)
        }
        return box
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Không mở được liên kết.", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        runTask++
        handler.removeCallbacksAndMessages(null)
        try { video?.stopPlayback() } catch (_: Exception) {}
        executor.shutdownNow()
        super.onDestroy()
    }
}
