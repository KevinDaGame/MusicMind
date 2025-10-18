package com.kevdadev.musicminds.ui.library

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kevdadev.musicminds.R
import com.kevdadev.musicminds.data.database.entities.LearningStatus
import kotlinx.coroutines.launch

class LibraryActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toLearnCountText: TextView
    private lateinit var learningCountText: TextView
    private lateinit var learnedCountText: TextView
    private val viewModel: LibraryViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Library"
        
        initializeViews()
        setupViewPager()
        observeStatistics()
    }
    
    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        toLearnCountText = findViewById(R.id.toLearnCount)
        learningCountText = findViewById(R.id.learningCount)
        learnedCountText = findViewById(R.id.learnedCount)
    }
    
    private fun setupViewPager() {
        val adapter = LibraryPagerAdapter(this)
        viewPager.adapter = adapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "To Learn"
                1 -> "Learning"
                2 -> "Learned"
                else -> ""
            }
        }.attach()
    }
    
    private fun observeStatistics() {
        lifecycleScope.launch {
            viewModel.categoryCounts.collect { counts ->
                toLearnCountText.text = counts.toLearnCount.toString()
                learningCountText.text = counts.learningCount.toString()
                learnedCountText.text = counts.learnedCount.toString()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh statistics when returning to the activity
        viewModel.refreshCategoryCounts()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private class LibraryPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            val status = when (position) {
                0 -> LearningStatus.TO_LEARN
                1 -> LearningStatus.LEARNING
                2 -> LearningStatus.LEARNED
                else -> LearningStatus.TO_LEARN
            }
            return LibraryFragment.newInstance(status)
        }
    }
}