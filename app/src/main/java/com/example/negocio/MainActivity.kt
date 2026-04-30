package com.example.negocio

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity() {

    private var selectedIndex = 2 // Inicio por defecto (centro)
    private val selectedScale = 1.25f
    private val liftDp = 18f
    private var isSplashReady = false

    private val navSelectedBgIds = listOf(
        R.id.nav_analisis_selected_bg,
        R.id.nav_simulador_selected_bg,
        R.id.nav_inicio_selected_bg,
        R.id.nav_mapa_selected_bg
    )

    private val navContainerIds = listOf(
        R.id.nav_analisis,
        R.id.nav_simulador,
        R.id.nav_inicio,
        R.id.nav_mapa
    )

    private val navIconIds = listOf(
        R.id.nav_icon_analisis,
        R.id.nav_icon_simulador,
        R.id.nav_icon_inicio,
        R.id.nav_icon_mapa
    )

    private val fragments: List<() -> Fragment> = listOf(
        { AnalisisFragment() },
        { SimuladorFragment() },
        { InicioFragment() },
        { MapFragment() },
        { PerfilFragment() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Instalar Splash Screen y configurarla para que dure 5 segundos
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isSplashReady }
        
        Handler(Looper.getMainLooper()).postDelayed({
            isSplashReady = true
        }, 2000)

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        // 2. Personalizar la salida de la Splash con un mensaje de "¡Bienvenido!"
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            
            // Crear el mensaje de bienvenida
            val welcomeText = TextView(this).apply {
                text = "¡Bienvenido!"
                textSize = 32f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_geobiz))
                gravity = Gravity.CENTER
                alpha = 0f
                translationY = 50f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Añadirlo al contenedor de la splash
            (splashView as? FrameLayout)?.addView(welcomeText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                topMargin = 400 // Ajustar según posición del logo
            })

            // Animación: El texto aparece, espera y luego todo se desliza hacia arriba
            val textFadeIn = ObjectAnimator.ofFloat(welcomeText, View.ALPHA, 0f, 1f).setDuration(800)
            val textSlideUp = ObjectAnimator.ofFloat(welcomeText, View.TRANSLATION_Y, 50f, 0f).setDuration(800)
            
            val exitSlideUp = ObjectAnimator.ofFloat(
                splashView,
                View.TRANSLATION_Y,
                0f,
                -splashView.height.toFloat()
            ).apply {
                duration = 600
                startDelay = 1500 // Tiempo para leer "¡Bienvenido!"
                interpolator = AnticipateInterpolator()
            }

            val exitFadeOut = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f).apply {
                duration = 400
                startDelay = 1500
            }

            AnimatorSet().apply {
                playTogether(textFadeIn, textSlideUp, exitSlideUp, exitFadeOut)
                doOnEnd { splashScreenViewProvider.remove() }
                start()
            }
        }

        findViewById<View>(R.id.btn_profile_top).setOnClickListener {
            if (selectedIndex < navContainerIds.size) {
                animateTabDeselect(selectedIndex)
            }
            selectedIndex = 4
            showFragment(4)
        }

        setupNavBar()
        if (savedInstanceState == null) {
            selectedIndex = 2
            navSelectedBgIds.forEachIndexed { i, id ->
                val circle = findViewById<View>(id)
                circle.visibility = if (i == 2) View.VISIBLE else View.GONE
                circle.scaleX = if (i == 2) 1f else 0f
                circle.scaleY = if (i == 2) 1f else 0f
                circle.translationY = if (i == 2) -dp(liftDp) else 0f
            }
            navIconIds.forEachIndexed { i, id ->
                val icon = findViewById<ImageView>(id)
                icon.scaleX = if (i == 2) selectedScale else 1f
                icon.scaleY = if (i == 2) selectedScale else 1f
                icon.translationY = if (i == 2) -dp(liftDp) else 0f
                setIconTint(icon, selected = i == 2)
            }
            showFragment(2)
        } else {
            selectedIndex = savedInstanceState.getInt("selected_index", 2)
            if (selectedIndex < navContainerIds.size) {
                navSelectedBgIds.forEachIndexed { i, id ->
                    val circle = findViewById<View>(id)
                    circle.visibility = if (i == selectedIndex) View.VISIBLE else View.GONE
                    circle.scaleX = if (i == selectedIndex) 1f else 0f
                    circle.scaleY = if (i == selectedIndex) 1f else 0f
                    circle.translationY = if (i == selectedIndex) -dp(liftDp) else 0f
                }
                navIconIds.forEachIndexed { i, id ->
                    val icon = findViewById<ImageView>(id)
                    icon.scaleX = if (i == selectedIndex) selectedScale else 1f
                    icon.scaleY = if (i == selectedIndex) selectedScale else 1f
                    icon.translationY = if (i == selectedIndex) -dp(liftDp) else 0f
                    setIconTint(icon, selected = i == selectedIndex)
                }
            }
            showFragment(selectedIndex)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_index", selectedIndex)
    }

    private fun setupNavBar() {
        navContainerIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener {
                selectTab(index)
            }
        }
    }

    private fun selectTab(index: Int) {
        if (index == selectedIndex) return

        if (selectedIndex < navContainerIds.size) {
            animateTabDeselect(selectedIndex)
        }
        
        selectedIndex = index
        animateTabSelect(index)
        showFragment(index)
    }

    private fun animateTabSelect(index: Int) {
        val circle = findViewById<View>(navSelectedBgIds[index])
        val icon = findViewById<ImageView>(navIconIds[index])

        circle.visibility = View.VISIBLE
        circle.scaleX = 0f
        circle.scaleY = 0f
        circle.translationY = 0f

        val lift = -dp(liftDp)
        val circleTranslateY = ObjectAnimator.ofFloat(circle, View.TRANSLATION_Y, 0f, lift)
        val circleScaleX = ObjectAnimator.ofFloat(circle, View.SCALE_X, 0f, 1f)
        val circleScaleY = ObjectAnimator.ofFloat(circle, View.SCALE_Y, 0f, 1f)
        circleTranslateY.duration = 280
        circleScaleX.duration = 280
        circleScaleY.duration = 280
        circleTranslateY.interpolator = OvershootInterpolator(1.15f)
        circleScaleX.interpolator = OvershootInterpolator(1.15f)
        circleScaleY.interpolator = OvershootInterpolator(1.15f)

        setIconTint(icon, selected = true)

        val iconTranslateY = ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, 0f, lift)
        val iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, selectedScale)
        val iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, selectedScale)
        iconTranslateY.duration = 250
        iconScaleX.duration = 250
        iconScaleY.duration = 250
        iconTranslateY.interpolator = OvershootInterpolator(1.2f)
        iconScaleX.interpolator = OvershootInterpolator(1.2f)
        iconScaleY.interpolator = OvershootInterpolator(1.2f)

        AnimatorSet().apply {
            playTogether(circleTranslateY, circleScaleX, circleScaleY, iconTranslateY, iconScaleX, iconScaleY)
            start()
        }
    }

    private fun animateTabDeselect(index: Int) {
        val circle = findViewById<View>(navSelectedBgIds[index])
        val icon = findViewById<ImageView>(navIconIds[index])

        val lift = -dp(liftDp)
        val circleTranslateY = ObjectAnimator.ofFloat(circle, View.TRANSLATION_Y, lift, 0f)
        val circleScaleX = ObjectAnimator.ofFloat(circle, View.SCALE_X, 1f, 0f)
        val circleScaleY = ObjectAnimator.ofFloat(circle, View.SCALE_Y, 1f, 0f)
        circleTranslateY.duration = 180
        circleScaleX.duration = 180
        circleScaleY.duration = 180

        setIconTint(icon, selected = false)

        val iconTranslateY = ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, lift, 0f)
        val iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, selectedScale, 1f)
        val iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, selectedScale, 1f)
        iconTranslateY.duration = 180
        iconScaleX.duration = 180
        iconScaleY.duration = 180

        AnimatorSet().apply {
            playTogether(circleTranslateY, circleScaleX, circleScaleY, iconTranslateY, iconScaleX, iconScaleY)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) { 
                    circle.visibility = View.GONE
                    circle.translationY = 0f
                }
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    private fun setIconTint(icon: ImageView, selected: Boolean) {
        val colorRes = if (selected) R.color.nav_bar_icon_selected else R.color.nav_bar_icon
        icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    /** Llamar desde fragmentos para cambiar de pestaña (0=Análisis, 1=Simulador, 2=Inicio, 3=Mapa, 4=Perfil). */
    fun navigateToTab(index: Int) {
        selectTab(index)
    }

    private fun showFragment(index: Int) {
        val profileButton = findViewById<View>(R.id.btn_profile_top)
        if (index == 4) { // Índice 4 corresponde a PerfilFragment
            profileButton.visibility = View.GONE
        } else {
            profileButton.visibility = View.VISIBLE
        }

        val fragment = fragments[index].invoke()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
        }
    }
}
