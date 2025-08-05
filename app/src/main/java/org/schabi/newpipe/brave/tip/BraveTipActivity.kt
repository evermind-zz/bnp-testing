package org.schabi.newpipe.brave.tip

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.ActivityBraveTipBinding
import org.schabi.newpipe.util.ThemeHelper

/**
 * Activity to load the Tip Fragment.
 */
class BraveTipActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.setTheme(this)
        title = getString(R.string.brave_tip_title)

        val tipActivityBinding = ActivityBraveTipBinding.inflate(layoutInflater)
        setContentView(tipActivityBinding.root)
        setSupportActionBar(tipActivityBinding.tipActivityToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.commit {
            replace(R.id.container, BraveTipFragment(), BraveTipFragment.NAME)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
