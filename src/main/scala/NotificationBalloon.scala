package org.bone.ircballoon

import org.eclipse.swt.widgets.{List => SWTList, _}
import org.eclipse.swt.layout._
import org.eclipse.swt.events._
import org.eclipse.swt.graphics._
import org.eclipse.swt.custom._

import org.eclipse.swt._
import scala.math._

trait NotificationBalloon
{
    this: BalloonController =>

    val FadeStep = 5
    val FadeTick = fadeTime / (alpha / 5)

    case class BalloonWindow(location: (Int, Int), bgColor: Color, borderColor: Color, 
                             message: IRCMessage) extends NotificationTheme 
                                                  with NotificationWindow
    {
        val display = Display.getDefault

        val uid = System.identityHashCode(this)
        val shell = new Shell(display, SWT.NO_TRIM|SWT.ON_TOP|SWT.RESIZE)
        val label = new StyledText(shell, SWT.MULTI|SWT.READ_ONLY|SWT.WRAP|SWT.NO_FOCUS)
    
        def calculateSize() =
        {
            val labelSize = label.computeSize(size._1 - 10, SWT.DEFAULT, true)
            (labelSize.x, labelSize.y)
        }

        def bottomY = {
            shell.getLocation.y + shell.getSize.y + spacing
        }

        def nicknameStyles(message: String): List[StyleRange] = 
        {
            val regex = """\w+:""".r

            regex.findAllIn(message).matchData.map { data => 
                val style = new StyleRange
                style.start = data.start
                style.length = data.end - data.start
                style.foreground = nicknameColor
                style.font = nicknameFont
                style
            }.toList
        }

        def opStyles(message: String): List[StyleRange] = 
        {
            val regex = """\[OP\] """.r

            regex.findAllIn(message).matchData.map { data => 
                val style = new StyleRange

                style.start = data.start
                style.length = data.end - data.start
                style.data = MyIcon.ircOP
                style.metrics = new GlyphMetrics(
                    MyIcon.ircOP.getBounds.height, 0, 
                    MyIcon.ircOP.getBounds.width / 4
                )

                style
            }.toList
        }

        def setLayout()
        {
            val layout = new GridLayout(1, false)
            val layoutData = new GridData(SWT.FILL, SWT.CENTER, true, true)
            layout.marginLeft = 5
            layout.marginRight = 5

            shell.setLayout(layout)
            label.setLayoutData(layoutData)
            label.setForeground(fontColor)
            label.setFont(font)
            label.setLineSpacing(5)
            label.setEnabled(false)
            label.setText(message.toString)
            label.addPaintObjectListener(new PaintObjectListener() {
                override def paintObject(event: PaintObjectEvent) {
                    event.style.data match {
                        case image: Image => 
                            val x = event.x
                            val y = event.y + event.ascent - event.style.metrics.ascent
                            event.gc.drawImage(image, x, y)

                        case _ =>
                    }
                }
            })

            nicknameStyles(message.toString).foreach { label.setStyleRange }
            opStyles(message.toString).foreach { label.setStyleRange }
        }

        def setSizeAndLocation()
        {
            val (width, height) = calculateSize()
            shell.setSize(width + 20, height + 20)
            shell.setLocation(location._1, location._2)
        }
    
        def prepare()
        {
            setBackground()
            setLayout()
            setSizeAndLocation()
            shell.setAlpha(0)
        }
    
        def open()
        {
            shell.open()
            fadeIn()
        }
        
        class FadeIn extends Runnable
        {
            override def run() 
            {
                if (shell.isDisposed) { return }
    
                val nextAlpha = min(255, shell.getAlpha + FadeStep)
    
                shell.setAlpha(nextAlpha)
    
                if (nextAlpha < alpha) {
                    Display.getDefault.timerExec(FadeTick, this)
                } else {
                    Display.getDefault.timerExec(displayTime, new FadeOut())
                }
            }
        }
    
        class FadeOut extends Runnable
        {
            override def run() {
    
                if (!shell.isDisposed) {
                    val nextAlpha = max(0, shell.getAlpha - FadeStep)
    
                    shell.setAlpha(nextAlpha)
    
                    if (nextAlpha > 0) {
                        Display.getDefault.timerExec(FadeTick, this)
                    } else {
                        removeNotification(BalloonWindow.this)
                    }
                }
            }
        }
    
        def fadeIn()
        {
            Display.getDefault.timerExec(FadeTick, new FadeIn)
        }
    }
}
