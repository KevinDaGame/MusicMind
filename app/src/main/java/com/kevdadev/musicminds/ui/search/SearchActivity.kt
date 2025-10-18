package com.kevdadev.musicminds.ui.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kevdadev.musicminds.R

class SearchActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Set up toolbar/action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Songs"
        }
        
        // Add the search fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchFragment.newInstance())
                .commit()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}