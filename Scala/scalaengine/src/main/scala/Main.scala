import scala.swing._
import scala.swing.event._
import java.awt.{Color, Graphics2D, BasicStroke}
import java.awt.geom._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.FileInputStream
import java.io.File
import javax.swing.border.Border
import java.awt

object GameEngine {

  def main(args: Array[String]): Unit = {
    var input = "Connect4"

    //Chess Array
    var chessImageArrayW = Array((1,"RookWhite"),(2,"KnightWhite"),(3,"BishopWhite"),(4,"QueenWhite"),
                                 (5,"KingWhite"),(3,"BishopWhite"),(2,"KnightWhite"),(1,"RookWhite"))
    var chessImageArrayB = Array((7,"RookBlack"),(8,"KnightBlack"),(9,"BishopBlack"),(10,"QueenBlack"),
                                 (11,"KingBlack"),(9,"BishopBlack"),(8,"KnightBlack"),(7,"RookBlack"))

    var chessBoard = Array.ofDim[Tuple2[Int,String]](8,8)
    chessBoard(0) = chessImageArrayW
    chessBoard(1) = Array.fill[Tuple2[Int,String]](8)(6,"PawnWhite")
    chessBoard(6) = Array.fill[Tuple2[Int,String]](8)(12,"PawnBlack")
    chessBoard(7) = chessImageArrayB  

    
    var drawBoard = (bgColor: Color,rows: Int,cols: Int,color1: Color,color2: Color,shape: String,g: Graphics2D) => {

      var width = 700;
      var height = 600;

      g.setColor(Color.WHITE)
      g.fillRect(0, 0, width, height)

      val tileSize = Math.min((width - 150) / cols, (height - 150) / rows)
      g.setColor(bgColor)
      g.fillRect(50, 50, tileSize * cols + 50, tileSize * rows + 50)

      g.setStroke(new BasicStroke(height / 100))
      val font = new Font("Arial", 0, 16)
      g.setFont(font)

      for {
        row <- 0 until rows
        col <- 0 until cols
      } {
        val x = (col * tileSize) + 75
        val y = (row * tileSize) + 75
        val tileColor = if ((row + col) % 2 == 0) color1 else color2
        g.setColor(tileColor)

        shape match {
          case "line" => {
            if (row != rows - 1 && col != cols - 1) {
              // changes color every 3 steps for soduko
              if (((row + 1) * (col + 1)) % 3 == 0) g.setColor(color1)
              else g.setColor(color2)
              g.drawLine(
                x + tileSize,
                75,
                x + tileSize,
                75 + (tileSize * (rows))
              )
              g.drawLine(
                75,
                y + tileSize,
                75 + (tileSize * (cols)),
                y + tileSize
              )
            }
          }
          case "square" => g.fillRect(x, y, tileSize, tileSize)
          case "circle" => g.fillOval(x, y, tileSize, tileSize)
          case _ => throw new IllegalArgumentException(s"Unsupported shape: $shape")
        }

        val aChar = ('a' + row).toChar
        val aString = s"$aChar"
        val oneString = s"${col + 1}"
        g.setColor(Color.BLACK)
        g.drawString(aString, 25, y + tileSize / 2)
        g.drawString(oneString, x + tileSize / 2, 25)
        g.drawString(aString, tileSize * cols + 125, y + tileSize / 2)
        g.drawString(oneString, x + tileSize / 2, tileSize * rows + 125)
      }
    }

    /*Check if a given coordinates is in the board or out*/
    def InBoard(input: String,rows:Int,cols:Int):Boolean = {

      if(input.size != 4)false
      
      var start:Tuple2[Int,Int] = (input(0)-'a'+1,input(1).asDigit)
      if(start._1 <= 0 || start._1 > cols )false
      if(start._2 <= 0 || start._2 > rows )false

      if(input.size > 2) {
        var end  :Tuple2[Int,Int] = (input(2)-'a'+1,input(3).asDigit)

        if(end._1 <= 0 || end._1 > cols )false
        if(end._2 <= 0 || end._2 > rows )false
      }

      true
    }

    val Chess_Controller = (input: String,rows:Int,cols:Int,turn:Int /*0->Black,1->white*/) => {

      //Check if Start and end Position are in Board*/
      var ret = true
      if(!InBoard(input,rows,cols))ret = false

      /*tuples containing position of peice in the Chess array (row,column)*/
      var start:Tuple2[Int,Int] = (Math.abs(input(1).asDigit-rows),input(0)-'a')
      var end  :Tuple2[Int,Int] = (Math.abs(input(3).asDigit-rows),input(2)-'a')
      
      /*if start peice is null then return false*/
      if(chessBoard(start._1)(start._2) == null)ret = false
      
      /*Check Validty of Start Position*/
      if(chessBoard(start._1)(start._2) != null)
      {
        /*if peice at start position is black and it's white turn then return false*/
        if(chessBoard(start._1)(start._2)._1 > 6 && turn == 1)ret = false

        /*if peice at start position is white and it's Black white turn then return false*/
        if(chessBoard(start._1)(start._2)._1 <= 6 && turn == 0)ret = false
      }

      /*Check Validty of end Position*/
      if(chessBoard(end._1)(end._2) != null)
      {
        /*if peice at end position is Black and it's Black turn then return false*/
        if(chessBoard(end._1)(end._2)._1 > 6 && turn == 0)ret = false

        /*if peice at end position is White and it's white turn then return false*/
        if(chessBoard(end._1)(end._2)._1 <= 6 && turn == 1)ret = false
      }      

      var peiceType = 14
      if(chessBoard(start._1)(start._2) != null){
        //Get the type of starting Peice
        peiceType = chessBoard(start._1)(start._2)._1
      }
  
      var deltaX = end._2 - start._2 
      var deltaY = end._1 - start._1
      var dirX = 0
      var dirY = 0

      //Get the direction Vector's X
      if(deltaX > 0)dirX = 1
      else if(deltaX < 0)dirX = -1

      //Get the direction Vector's Y
      if(deltaY > 0)dirY = 1
      else if(deltaY < 0)dirY = -1

      def rookMove():Boolean={

        var accept = true
        //if both Change in X and Change in Y are not zero then this move is Invalid
        if(Math.abs(deltaY) > 0 && Math.abs(deltaX) > 0)accept = false

        var startY = start._1
        var startX = start._2
        
        
        //go in the direction of the Move
        while((startY != end._1 || startX != end._2) && accept){
          startY += dirY
          startX +=dirX
          
          //if there is any peice in the pass then this move is InValid So return false
          if((chessBoard(startY)(startX) != null) && (startY != end._1 || startX != end._2)) 
            accept = false         
        }

        accept
      }

      def knightMove():Boolean={

        /*Valid Moves for knight are only those whose cahnge in one of its dimensions is 2 
          and the other is 1, otherwise this Move is In valid*/
        if((Math.abs(deltaY) == 2 && Math.abs(deltaX) == 1) || 
          (Math.abs(deltaY) == 1 && Math.abs(deltaX) == 2))
          true
        else
          false 
      }

      def bishopMove():Boolean={
        
        var accept = true
        /*Valid Moves for Bishop are only Moves Where absolute Change in x equals absolute Change in Y*/
        if(Math.abs(deltaY) == Math.abs(deltaX))
        {
          var startY = start._1
          var startX = start._2
        
          //go in the direction of the Move
          while(startY != end._1 || startX != end._2){
            startY += dirY
            startX +=dirX
            
            //if there is any peice in the pass then this move is InValid So return false
            if((chessBoard(startY)(startX) != null) && (startY != end._1 || startX != end._2)) 
              accept = false         
          }
        }
        else
          accept = false

        accept
      }

      def queenMove():Boolean={

        var accept = true
        /*only the none acceptable Move is the Move when change in x and Y are not equal and none of them are zeros*/
        if(Math.abs(deltaY) > 0 && Math.abs(deltaX) > 0 && Math.abs(deltaY) != Math.abs(deltaX))accept = false
        
        var startY = start._1
        var startX = start._2
        
        //go in the direction of the Move
        while((startY != end._1 || startX != end._2) && accept)
        {
          startY +=dirY
          startX +=dirX
            
          //if there is any peice in the pass then this move is InValid So return false
          if((chessBoard(startY)(startX) != null) && (startY != end._1 || startX != end._2)) 
              accept = false         
        }

        accept
      }

      def kingMove():Boolean={
        var accept = true

        if(Math.abs(deltaY)<2 && Math.abs(deltaX)<2){
          var startY = start._1+deltaY
          var startX = start._2+deltaX

          for{
            i <- List(0,-1,1)
            j <- List(0,-1,1)
          }{
            if(i>=0 && i<8 && j>=0 && j<8 && (i != 0 || j != 0)){
              
              if(chessBoard(startY+i)(startX+j)._1 <= 6 && turn == 0)accept = false
              if(chessBoard(startY+i)(startX+j)._1 > 6 && turn == 1)accept = false
              
            }
          }
        }
        else{
          accept = false
        }

        accept
      }

      def pawnMove():Boolean={

        var accept = false
        var startY = start._1
        var startX = start._2

        if(turn == 0){
          //Black pawn moves up by 1
          if(deltaY == -1 && deltaX == 0 && chessBoard(startY-1)(startX) == null)accept = true 
          //Black pawn moves diagonally left by 1
          if(deltaY == -1 && deltaX == -1 && chessBoard(startY-1)(startX-1) != null)accept = true
          //Black pawn moves diagonally right by 1
          if(deltaY == -1 && deltaX == 1 && chessBoard(startY-1)(startX+1) != null)accept = true
          //Black pawn moves up by 2 at first move
          if(deltaY == -2 && startY == 6 && deltaX == 0 && chessBoard(startY-2)(startX) == null && chessBoard(startY-1)(startX) == null)accept = true
        }
        else{
          //White pawn moves up by 1
          if(deltaY == 1 && deltaX == 0 && chessBoard(startY+1)(startX) == null)accept = true
          //White pawn moves diagonally left by 1
          if(deltaY == 1 && deltaX == -1 && chessBoard(startY+1)(startX-1) != null)accept = true
          //White pawn moves diagonally right by 1
          if(deltaY == 1 && deltaX == 1 && chessBoard(startY+1)(startX+1) != null)accept = true
          //White pawn moves up by 2 at first move
          if(deltaY == 2 && startY == 1 && deltaX == 0 && chessBoard(startY+2)(startX) == null && chessBoard(startY+1)(startX) == null)accept = true
        }
      
        accept
      }

      var valid = peiceType match{
        case 1|7  => rookMove
        case 2|8  => knightMove
        case 3|9  => bishopMove
        case 4|10 => queenMove
        case 5|11 => kingMove
        case 6|12 => pawnMove
        case _ => false
      }
      
      if(ret){  
        if(valid )true
        else false
      }
      else{
        false
      }

    }: Boolean

    val Connect4_Controller = (input: String,rows:Int,cols:Int,Turn:Int) => {true}: Boolean

    val XO_Controller = (input: String,rows:Int,cols:Int,Turn:Int) => {true}: Boolean

    val Checkers_Controller = (input: String,rows:Int,cols:Int,Turn:Int) => {true}: Boolean

    val Queens_Controller = (input: String,rows:Int,cols:Int,Turn:Int) => {true}: Boolean

    val Suduko_Controller = (input: String,rows:Int,cols:Int,Turn:Int) => {true}: Boolean

    new MainFrame {
      title = "Game Engine"

      val Games: List[String] = List("Chess", "Connect4", "XO", "Checkers", "Suduko", "8Queens")

      contents = new BoxPanel(Orientation.Vertical) {

        def readImage(img:String):BufferedImage = {
            val file = new File("src/main/resources/MainMenu/"+img+".jpg")
            val image = ImageIO.read(file)
            image
        }

        var j = 0;
        override def paint(g: Graphics2D): Unit = {
          var myFont = new Font("Courier New", 1, 20);
          g.setFont(myFont)

          g.setBackground(new Color(0xE5E1E6))
          g.clearRect(0,0,700,650);
          g.setColor(new Color(200,100,250))
          g.drawImage(readImage(Games(j)),100,20,500,400,null)

          var i = 0;
          Games.foreach(x => {
            g.drawString(x, 300, 450 + 30 * i)
            i += 1
          })
          
          g.drawString(">", 270, 450 + 30 * j)
        }

        listenTo(keys)
        listenTo(mouse.clicks)

        reactions += {
          case MouseClicked(_, p, _, _, _) =>
            println(p)
            requestFocus()
          case KeyPressed(_, c, _, _) => c match {
            
            case Key.S =>{
              j+=1;
              if(j == 6)j=0;
              repaint()
            }

            case Key.W =>{
              j-=1;
              if(j < 0)j=5;
              repaint()
            }

            case Key.Enter =>{
              input = Games(j);
              
              input match {
                  case "Chess"    => abstractEngine(Color.GRAY, 8, 8, new Color(0xC2B280), Color.WHITE, "square",2,input,drawBoard,Chess_Controller)
                  case "8Queens"  => abstractEngine(Color.DARK_GRAY, 8, 8, new Color(0xC2B280), Color.WHITE, "square",2,input,drawBoard,Queens_Controller)
                  case "Connect4" => abstractEngine(Color.BLUE, 6, 8, Color.WHITE, Color.WHITE, "circle",1,input,drawBoard,Connect4_Controller)
                  case "XO"       => abstractEngine(Color.BLACK, 3, 3, Color.YELLOW, Color.YELLOW, "line",1,input,drawBoard,XO_Controller)
                  case "Suduko"   => abstractEngine(Color.LIGHT_GRAY, 9, 9, Color.BLACK, Color.WHITE, "line",1,input,drawBoard,Suduko_Controller)
                  case "Checkers" => abstractEngine(Color.DARK_GRAY, 8, 8, new Color(0xC2B280), Color.WHITE,"square",2,input,drawBoard,Checkers_Controller)
              }

              dispose()
            }
            requestFocus()
          }
        }
      }

      size = new Dimension(700, 650)
      background = new Color(0xf2d16b)
      centerOnScreen
      visible = true
    }

    def abstractEngine(
        bgColor: Color,rows: Int,cols: Int,color1: Color,color2: Color,
        shape: String,typ:Int,gameName:String,
        Drawer: (Color,Int,Int,Color,Color,String,Graphics2D) => Unit,
        Controller: (String,Int,Int,Int) => Boolean
    ): Unit = {
      new MainFrame(null) {
        title = "Game Engine"
        class Canvas extends Component {
          preferredSize = new Dimension(700, 600)
          override def paint(g: Graphics2D): Unit = {

            Drawer(bgColor,rows,cols,color1,color2,shape,g);
            
            for {
              row <- 0 until 8
              col <- 0 until 8
            } {
              val x = (col * 57) + 75
              val y = (row * 57) + 75
              if(chessBoard(row)(col) != null) g.drawImage(readImage(chessBoard(row)(col)._2),x,y,50,50,null)
            }
          }       
          
          def readImage(img:String):BufferedImage = {
            val file = new File("src/main/resources/Chess/"+img+".png")
            val image = ImageIO.read(file)
            image
          }
        }  

        //GUI
        contents = new BoxPanel(Orientation.Vertical) {

          var canvas = new Canvas
          contents += canvas
          contents += Swing.Glue
          contents += new FlowPanel{
  
            contents += new BoxPanel(Orientation.NoOrientation){
              maximumSize = new Dimension(670,20)
              preferredSize = new Dimension(670,20)
              background = new Color(0xb1e9fe)

              var str = ""
              if(typ == 1)str = "Position"
              else str = "Start Position"

              contents += Swing.HStrut(10)          
              contents += new Label(str){font = new Font("Segoe Print", 1, 16)}
              contents += Swing.HStrut(27)
              if(typ == 2)contents += new Label("end Position"){font = new Font("Segoe Print", 1, 16)}
            }

            contents += new BoxPanel(Orientation.NoOrientation){
              maximumSize = new Dimension(670,20)
              preferredSize = new Dimension(670,20)
              background = new Color(0xb1e9fe)

              var txt = gameName match{
                case "Chess"|"8Queens"|"Checkers" => "Black's  Turn"
                case "XO" | "Connect4" => "Player's 1 Turn"
              }

              var inputField1 = new TextField("")
              var inputField2 = new TextField("")
              var label = new Label(txt)
              //Adjust Size
              inputField1.maximumSize = new Dimension(100,30)
              inputField2.maximumSize = new Dimension(100,30)
              //Adjust Font
              inputField1.font = new Font("Arial", 0, 15)
              inputField2.font = new Font("Arial", 0, 15)
              //label adj
              label.maximumSize = new Dimension(100,20)
              label.font = new Font("Arial", 1, 15)
              label.foreground = new Color(0x013220)
             
              contents += Swing.HStrut(15)
              contents += inputField1
              contents += Swing.HStrut(35)
              
              if(typ == 2)contents += inputField2
              else contents += Swing.HStrut(100)

              contents += Swing.HStrut(80)
              contents += label
              contents += Swing.HStrut(140)

              var turn = 0
              contents += Button("Do Action!!") { 
                  var s1 = inputField1.text
                  var s2 = inputField2.text
                  var input = s1+s2
                  println(input) 

                 
                  if(Controller(input,rows,cols,turn)){

                    println("OK")
                    println(Math.abs(s2(1).asDigit-rows),s2(0)-'a')
                    println(Math.abs(s1(1).asDigit-rows),s1(0)-'a')
                    chessBoard(Math.abs(s2(1).asDigit-rows))(s2(0)-'a') = chessBoard(Math.abs(s1(1).asDigit-rows))(s1(0)-'a')
                    chessBoard(Math.abs(s1(1).asDigit-rows))(s1(0)-'a') = null

                    turn+=1
                    turn = turn%2  

                    if(turn%2 == 0){
                      gameName match{
                        case "Chess"|"8Queens"|"Checkers" => label.text = "Black's  Turn"
                        case "XO" | "Connect4" => label.text = "Player's 1 Turn"
                      }
                    } 
                    else {
                      gameName match{
                        case "Chess"|"8Queens"|"Checkers" => label.text = "White's  Turn"
                        case "XO" | "Connect4" => label.text = "Player's 2 Turn"
                      }
                    }
                    //Reset Input Fields After each Move
                    inputField1.text =""
                    inputField2.text =""

                    canvas.repaint()
                  }
                  else{
                    label.foreground = Color.RED
                    label.text = "Not valid"
                    label.foreground = new Color(0x013220)
                  }       
              }
            }

            background = new Color(0xb1e9fe)
            maximumSize = new Dimension(700,80)
            preferredSize = new Dimension(700,80)
            border = Swing.TitledBorder(Swing.EtchedBorder(Swing.Lowered), "Input")
          } 
        }
        bounds = new Rectangle(700, 700)
        centerOnScreen()
        resizable = false
        visible = true
      }
    }
  }
}