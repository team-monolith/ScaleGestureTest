package com.example.scalegesturetest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import kotlin.random.Random

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private lateinit var mScaleDetector: ScaleGestureDetector

    val handler = Handler()//メインスレッド処理用ハンドラ

    var moveview: MoveView? = null //キャンバスリフレッシュ用インスタンス保持変数

    lateinit var MAPDATA: Bitmap

    data class COORDINATE(var X: Float?, var Y: Float?)


    var scale: Float = 1F   //地図表示のスケール,1.5-3.0-4.5

    var pos: COORDINATE = COORDINATE(0f, 0f)
    var log: COORDINATE = COORDINATE(0f, 0f)

    var touchPointX: Float = 0f
    var touchPointY: Float = 0f


    var size: Rect? = null  //画面サイズ取得用


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_first, container, false)
        val layout = view.findViewById<ConstraintLayout>(R.id.constlayout)
        moveview = MoveView(this.activity)

        layout.addView(moveview)
        layout.setWillNotDraw(false)

        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MAPDATA = CreateBitmap()

        HandlerDraw(moveview!!)

        //画面サイズ取得
        size = Rect()
        activity?.window?.decorView?.getWindowVisibleDisplayFrame(size)

        //ビューにリスナーを設定
        view.setOnTouchListener { _, event ->
            onTouch(view, event)
        }


    }

    //描画関数　再描画用
    fun HandlerDraw(mv: MoveView) {
        handler.post(object : Runnable {
            override fun run() {
                SystemRefresh()
                //再描画
                mv.invalidate()
                handler.postDelayed(this, 0)
            }
        })
    }

    fun SystemRefresh() {
    }

    fun CreateBitmap(): Bitmap {
        val output = Bitmap.createBitmap(5000, 5000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        for (y in 0..499) {
            for (x in 0..499) {
                val rect = Rect(x * 10, y * 10, x * 10 + 10, y * 10 * 10)
                paint.color = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
                canvas.drawRect(rect, paint)
            }
        }
        return output
    }

    //タッチイベント実行時処理
    fun onTouch(view: View, event: MotionEvent): Boolean {

        //複数本タッチの場合はピンチ処理
        if (event.pointerCount > 1) {
            mScaleDetector.onTouchEvent(event)
        }

        //一本指タッチの場合は画面移動処理
        else {
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    log.X = event.x
                    log.Y = event.y
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    pos.X = pos.X!! + event.x - log.X!!
                    pos.Y = pos.Y!! + event.y - log.Y!!
                    log.X = event.x
                    log.Y = event.y
                }
                event.action == MotionEvent.ACTION_UP -> {
                }
            }
        }

        return true
    }

    //タッチしている場所の座標が、canvas上のどこの座標なのかを返す
    fun TouchCoordinate(touch: COORDINATE): COORDINATE {
        val x = -pos.X!! + touch.X!!
        val y = -pos.Y!! + touch.Y!!
        return COORDINATE(x, y)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        //ピンチ処理関係
        mScaleDetector = ScaleGestureDetector(context,
                object : ScaleGestureDetector.OnScaleGestureListener {

                    //スケール変更処理
                    override fun onScale(detector: ScaleGestureDetector): Boolean {

                        scale *= mScaleDetector.scaleFactor

                        return true
                    }

                    //ピンチ開始時処理
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        touchPointX = detector.getFocusX()
                        touchPointY = detector.getFocusY();
                        return true
                    }

                    //ピンチ終了時処理
                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                    }

                }
        )
    }


    inner class MoveView : View {
        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
                context,
                attrs,
                defStyleAttr
        )

        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)


            val paint = Paint()

            //画面中心の座標を取得（仮置き）
            val Center: COORDINATE = TouchCoordinate(COORDINATE(size!!.width() / 2f, size!!.height() / 2f))

            paint.color = Color.parseColor("#FF0000")


            //何も弄っていないキャンバスのデータを保存
            canvas!!.save()

            //地図の移動
            canvas.translate(pos.X!!, pos.Y!!)

            //地図の拡大縮小
            canvas.scale(scale, scale, Center.X!!,Center.Y!!)

            //地図の描画
            canvas.drawBitmap(MAPDATA, 0f, 0f, paint)


            //画面中心の円を描画
            canvas.drawCircle(Center.X!!, Center.Y!!, 50*(1/scale), paint)

            //キャンバスの移動と拡大縮小をリセット
            canvas.restore()

            //デバッグ用の文字サイズと色を設定
            paint.textSize = 80f
            paint.color = Color.parseColor("#FFFFFF")

            //座標を描画（デバッグ用）
            canvas.drawText("S=$scale", 0f, 80f, paint)
            canvas.drawText("X=" + pos.X, 0f, 160f, paint)
            canvas.drawText("y=" + pos.Y, 0f, 240f, paint)
        }
    }
}