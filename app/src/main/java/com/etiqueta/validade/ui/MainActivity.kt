package com.etiqueta.validade.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.etiqueta.validade.R
import com.etiqueta.validade.databinding.ActivityMainBinding
import com.etiqueta.validade.ui.etiqueta.EtiquetaLivreFragment
import com.etiqueta.validade.ui.etiqueta.ValidadeFragment
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupTabs()
    }

    private fun setupTabs() {
        val tabs = binding.tabLayout
        tabs.addTab(tabs.newTab().setText("🏷 Validade"))
        tabs.addTab(tabs.newTab().setText("✏️ Etiqueta Livre"))

        showFragment(ValidadeFragment())

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showFragment(ValidadeFragment())
                    1 -> showFragment(EtiquetaLivreFragment())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> { startActivity(Intent(this, ConfiguracaoActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }
}
