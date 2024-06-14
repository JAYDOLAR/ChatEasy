package CustomViews

import Utility.CustomViewUtility
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PointF
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.chateasy.R
import kotlin.math.hypot

class Material3SearchToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ConstraintLayout(context, attrs, defStyleAttr) {
    // Listener interface for search events
    interface Listener {
        fun onSearchTextChange(text: String?)
        fun onSearchClosed()
    }

    private var listener: Listener? = null
    private val input: EditText
    private val circularRevealPoint = PointF()

    init {
        inflate(context, R.layout.material3_serarch_toolbar, this)

        // Initialize views
        input = findViewById(R.id.search_input)
        val close = findViewById<View>(R.id.search_close)
        val clear = findViewById<View>(R.id.search_clear)

        // Set click listeners
        close.setOnClickListener { collapse() }
        clear.setOnClickListener {
            input.setText(
                ""
            )
        }

        // Add text change listener
        input.addTextChangedListener(object : CustomTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                clear.visibility =
                    if (s.toString().trim { it <= ' ' }.isNotEmpty()) VISIBLE else GONE
                if (listener != null) {
                    listener!!.onSearchTextChange(s.toString())
                }
            }
        })
    }

    // Set hint for search input field
    fun setSearchInputHint(hintStringRes: Int) {
        input.setHint(hintStringRes)
    }

    // Show search toolbar with circular reveal animation
    fun display(x: Float, y: Float) {
        if (visibility != VISIBLE) {
            circularRevealPoint[x] = y
            val cx = x.toInt()
            val cy = y.toInt()
            val finalRadius =
                hypot(width.toDouble(), height.toDouble()).toFloat()
            val animator = ViewAnimationUtils.createCircularReveal(this, cx, cy, 0f, finalRadius)
            animator.setDuration(400)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = VISIBLE
                    // Request focus and show keyboard when search toolbar is shown
                    CustomViewUtility.focusAndShowKeyboard(input)
                }
            })
            animator.start()
        }
    }

    // Collapse search toolbar with circular reveal animation
    private fun collapse() {
        if (visibility == VISIBLE) {
            if (listener != null) {
                listener!!.onSearchClosed()
            }
            // Hide keyboard when search toolbar is collapsed
            CustomViewUtility.hideKeyboard(context, input)
            val cx = circularRevealPoint.x.toInt()
            val cy = circularRevealPoint.y.toInt()
            val initialRadius =
                hypot(width.toDouble(), height.toDouble()).toFloat()
            val animator = ViewAnimationUtils.createCircularReveal(this, cx, cy, initialRadius, 0f)
            animator.setDuration(400)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = INVISIBLE
                }
            })
            animator.start()
        }
    }

    // Clear text in search input field
    fun clearText() {
        input.setText("")
    }

    // Set listener for search events
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    // CustomTextWatcher class for handling text changes
    private abstract class CustomTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }
}
