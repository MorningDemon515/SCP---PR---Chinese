Const C_GWL_STYLE = -16
Const C_WS_POPUP = $80000000
Const C_HWND_TOP = 0
Const C_SWP_SHOWWINDOW = $0040

Global versionnumber$ = "2.0"

Const ClrR = 50, ClrG = 50, ClrB = 50

Global MouseDown1%, MouseHit1%, MouseDown2%, MouseSpeedX#, MouseSpeedY#, MouseSpeedZ#
Global SelectedTextBox% = 0
Global PrevSelectedTextBox% = 0

Global AspectRatio# = 16.0/9.0

Global ResWidth = 895
Global ResHeight = 560
Global ResFactor# = ResHeight/768.0

Graphics3D ResWidth,ResHeight,0,2
hwnd = api_GetActiveWindow() ;User32.dll
api_ShowWindow% (hwnd ,0)    ;User32.dll
SetBuffer BackBuffer()

Global G_app_handle = SystemProperty( "AppHWND" )
;; -- Change the window style to 'WS_POPUP' and then set the window position to force the style to update.
api_SetWindowLong( G_app_handle, C_GWL_STYLE, C_WS_POPUP )
api_SetWindowPos( G_app_handle, C_HWND_TOP, 0, 0, ResWidth, ResHeight, C_SWP_SHOWWINDOW )

AppTitle "MapCreator 3d view"

Global Camera = CreateCamera()
Global CamColorR% = GetINIInt("options.INI","3d scene","bg color R")
Global CamColorG% = GetINIInt("options.INI","3d scene","bg color G")
Global CamColorB% = GetINIInt("options.INI","3d scene","bg color B")
Global CursorColorR% = GetINIInt("options.INI","3d scene","cursor color R")
Global CursorColorG% = GetINIInt("options.INI","3d scene","cursor color G")
Global CursorColorB% = GetINIInt("options.INI","3d scene","cursor color B")
CameraClsColor Camera,CamColorR,CamColorG,CamColorB
Global CamRange# = GetINIFloat("options.INI","3d scene","camera range")
CameraRange Camera,0.05,CamRange
PositionEntity Camera,0,1,0

Global AmbientLightRoomTex% = CreateTexture(2,2,257)
TextureBlend AmbientLightRoomTex,5
SetBuffer(TextureBuffer(AmbientLightRoomTex))
ClsColor 0,0,0
Cls

SetBuffer BackBuffer()

Global MenuOpen% = True
Global RandomSeed$ = "testseed"

Const ROOM1% = 1, ROOM2% = 2, ROOM2C% = 3, ROOM3% = 4, ROOM4% = 5

Global Font1 = LoadFont("..\GFX\cour.ttf", 16)

Global RoomTempID%

Global FileLocation$ = "..\Data\rooms.ini"
Global FileLocationPR$ = "..\ProjectR\Data\rooms.ini"
LoadRoomTemplates(FileLocation)
LoadRoomTemplates(FileLocationPR)
LoadMaterials("..\Data\materials.ini")

If RandomSeed = "" Then
	RandomSeed = Abs(MilliSecs())
EndIf
;Local strtemp$ = ""
;For i = 1 To Len(RandomSeed)
;	strtemp = strtemp+Asc(Mid(RandomSeed,i,1))
;Next
;SeedRnd Abs(Int(strtemp))

Global RoomScale# = 8.0 / 2048.0
Const ZONEAMOUNT = 3
Global MapWidth% = GetINIInt("..\options.ini", "options", "map size"), MapHeight% = GetINIInt("..\options.ini", "options", "map size")
Dim MapTemp%(MapWidth+1, MapHeight+1)
Dim MapFound%(MapWidth, MapHeight)

Dim MapName$(MapWidth, MapHeight)
Dim MapRoomID%(ROOM4 + 1)
Dim MapRoom$(ROOM4 + 1, 0)

Global PickedRoom.Rooms

ChangeDir ".."

;CreateMap()
LoadRoomTemplateMeshes()

FreeTextureCache

;PlaceRoom("start",MapWidth/2-1,MapHeight,0,1,ROOM1,"alarm")

ChangeDir "Map Creator"

Global ShowFPS% = GetINIInt("options.ini", "3d scene", "show fps")
Global CheckFPS%, ElapsedLoops%, FPS%
Global VSync% = GetINIInt("options.ini", "3d scene", "vsync")

Global MXS#=0.0,MYS#=0.0
MoveMouse GraphicsWidth()/2,GraphicsHeight()/2
Global mouse_left_limit% = 250, mouse_right_limit% = GraphicsWidth () - 250
Global mouse_top_limit% = 150, mouse_bottom_limit% = GraphicsHeight () - 150

Global Faster% = False
Global Slower% = False
Global IsRemote% = True

PositionEntity Camera,(MapWidth/2)*8,1,(MapHeight)*8
RotateEntity Camera,0,180,0
MXS#=180

Const GameUPS=60 ; Updates per second
Global Period#=1000.0/GameUPS

Global PrevTime% = MilliSecs2()
Global ElapsedTime#

Repeat
	Cls
	If ShowFPS
		If CheckFPS < MilliSecs2() Then
			FPS = ElapsedLoops
			ElapsedLoops = 0
			CheckFPS = MilliSecs2()+1000
		EndIf
		ElapsedLoops = ElapsedLoops + 1
	EndIf
	
	ElapsedTime = ElapsedTime+Float(MilliSecs2()-PrevTime)/Float(Period)
	PrevTime = MilliSecs2()
	
	Local f%
	If FileType("CONFIG_OPTINIT.SI")=1
		f = ReadFile("CONFIG_OPTINIT.SI")
		
		CamColorR = ReadInt(f)
		CamColorG = ReadInt(f)
		CamColorB = ReadInt(f)
		CursorColorR = ReadInt(f)
		CursorColorG = ReadInt(f)
		CursorColorB = ReadInt(f)
		CamRange = ReadInt(f)
		VSync = ReadByte(f)
		ShowFPS = ReadByte(f)
		
		CamRange = Max(CamRange,20)
		
		CameraClsColor Camera,CamColorR,CamColorG,CamColorB
		CameraRange Camera,0.05,CamRange*2
		
		CloseFile f
		DeleteFile("CONFIG_OPTINIT.SI")
	EndIf
	
	Local x,y
	Local r.Rooms,rt.RoomTemplates
	Local name$,angle
	Local ename$,eprob#
	If FileType("CONFIG_MAPINIT.SI")=1
		ClearTextureCache
		For r.Rooms = Each Rooms
			FreeEntity r\obj
			FreeTexture r\overlaytex
			Delete r
		Next
		
		f = ReadFile("CONFIG_MAPINIT.SI")
		
		While Not Eof(f)
			x = ReadByte(f)
			y = ReadByte(f)
			name$ = Lower(ReadString(f))
			
			angle = ReadByte(f)*90.0
			
			ename = ReadString(f)
			eprob# = ReadFloat(f)
			
			For rt.RoomTemplates=Each RoomTemplates
				If Lower(rt\Name) = name
					If angle<>90 And angle<>270
						angle = angle - 180
					EndIf
					
					PlaceRoom(name,MapWidth-x,y,angle,GetZone(y),rt\Shape,ename,eprob)
					
					Exit
				EndIf
			Next
			
			Local isSelRoom% = ReadByte(f)
			If isSelRoom%
				PositionEntity Camera,(MapWidth-x)*8,1,y*8
				RotateEntity Camera,0,angle,0
				MXS = -angle
				MYS = 0
			EndIf
			
		Wend
		
		FreeTextureCache
		
		CloseFile f
		DeleteFile("CONFIG_MAPINIT.SI")
	EndIf
	
	While ElapsedTime>0.0
		If MouseHit(2)
			IsRemote = Not IsRemote
			MoveMouse GraphicsWidth()/2,GraphicsHeight()/2
		EndIf
		
		For r = Each Rooms
			If r\resetoverlaytex And r<>PickedRoom
				SetBuffer TextureBuffer(r\overlaytex)
				ClsColor 0,0,0
				Cls
				SetBuffer BackBuffer()
				r\resetoverlaytex=False
			EndIf
			PickedRoom = Null
			If EntityDistance(Camera,r\obj)>(CamRange) Or (Not EntityInView(GetChild(r\obj,2),Camera))
				HideEntity r\obj
			Else
				ShowEntity r\obj
			EndIf
		Next
		
		If (Not IsRemote)
			HidePointer()
			
			If (MouseX() > mouse_right_limit) Or (MouseX() < mouse_left_limit) Or (MouseY() > mouse_bottom_limit) Or (MouseY() < mouse_top_limit)
				MoveMouse GraphicsWidth()/2,GraphicsHeight()/2
			EndIf
			
			MXS# = MXS# + MouseXSpeed()*0.25
			MYS# = MYS# + MouseYSpeed()*0.25
			
			RotateEntity Camera,MYS,-MXS,0
			
			Faster = 0
			If KeyDown(42) Then Faster = 1
			Slower = 0
			If KeyDown(29) Then Slower = 1
			
			If KeyDown(17) Then MoveEntity Camera,0,0,((0.05)+(0.05*Faster))/(1+Slower)
			If KeyDown(30) Then MoveEntity Camera,(-0.05-(0.05*Faster))/(1+Slower),0,0
			If KeyDown(31) Then MoveEntity Camera,0,0,(-0.05-(0.05*Faster))/(1+Slower)
			If KeyDown(32) Then MoveEntity Camera,(0.05+(0.05*Faster))/(1+Slower),0,0
			
			Local picker% = EntityPick(Camera,CamRange/2.5)
			If picker<>0
				For r = Each Rooms
					If PickedEntity()=GetChild(r\obj,2)
						SetBuffer TextureBuffer(r\overlaytex)
						ClsColor 70,70,20+(Sin(MilliSecs2()/4.0)*20)
						Cls
						SetBuffer BackBuffer()
						PickedRoom = r
						r\resetoverlaytex=True
						Exit
					EndIf
				Next
			EndIf
		Else
			ShowPointer()
		EndIf
		
		ElapsedTime=ElapsedTime-1.0 ;indicate that a frame has been processed
		
		CaptureWorld ;capture this game state, tweening will make it look smooth
	Wend
	RenderWorld 1.0-Max(Min(ElapsedTime,0.0),-1.0)
	
	If (Not IsRemote)
		SetFont Font1
		If ShowFPS
			Color 0,0,0
			Rect 2,2,StringWidth("FPS: "+FPS),StringHeight("FPS: "+FPS)
			
			Color 255,255,255
			Text 2,2,"FPS: "+FPS
		EndIf
		
		If PickedRoom<>Null
			Local rname$ = PickedRoom\RoomTemplate\Name
			Local rX% = Int(PickedRoom\x)
			Local rZ% = Int(PickedRoom\z)
			
			Color 0,0,0
			Rect 2,32,StringWidth("Room name: "+rname),StringHeight("Room name: "+rname)
			Rect 2,52,StringWidth("Room X: "+rX),StringHeight("Room X: "+rX)
			Rect 2,72,StringWidth("Room Z: "+rZ),StringHeight("Room Z: "+rZ)
			
			Color 255,255,255
			Text 2,32,"Room name: "+rname
			Text 2,52,"Room X: "+rX
			Text 2,72,"Room Z: "+rZ
			
			If PickedRoom\event<>""
				ename$ = PickedRoom\event
				Local echance# = PickedRoom\eventchance
				
				Color 0,0,0
				Rect 2,92,StringWidth("Room event: "+ename),StringHeight("Room event: "+ename)
				Rect 2,112,StringWidth("Room event chance: "+Int(echance*100)+"%"),StringHeight("Room event chance: "+Int(echance*100)+"%")
				
				Color 255,255,255
				Text 2,92,"Room event: "+ename
				Text 2,112,"Room event chance: "+Int(echance*100)+"%"
			EndIf
		EndIf
		
		Color CursorColorR,CursorColorG,CursorColorB
		Rect (ResWidth/2)-25,(ResHeight/2)-2.5,20,5,True
		Rect (ResWidth/2)+5,(ResHeight/2)-2.5,20,5,True
		Rect (ResWidth/2)-2.5,(ResHeight/2)-25,5,20,True
		Rect (ResWidth/2)-2.5,(ResHeight/2)+5,5,20,True
	EndIf
	
	If VSync
		Flip True
	Else
		Flip False
	EndIf
Until api_FindWindow( "BlitzMax_Window_Class" , "SCP-CB: PR - Map Creator "+versionnumber) = 0
End





Type RoomTemplates
	Field obj%, id%
	Field objPath$
	
	Field zone%[5]
	
	Field Shape%, Name$
	Field Commonness%, Large%
	Field DisableDecals%
End Type

Function CreateRoomTemplate.RoomTemplates(meshpath$)
	Local rt.RoomTemplates = New RoomTemplates
	
	rt\objPath = meshpath
	
	rt\id = RoomTempID
	RoomTempID=RoomTempID+1
	
	Return rt
End Function

Function LoadRoomTemplates(file$)
	Local TemporaryString$, i%
	Local rt.RoomTemplates = Null
	Local StrTemp$ = ""
	
	Local f = OpenFile(file)
	
	While Not Eof(f)
		TemporaryString = Trim(ReadLine(f))
		If Left(TemporaryString,1) = "[" Then
			TemporaryString = Mid(TemporaryString, 2, Len(TemporaryString) - 2)
			StrTemp = GetINIString(file, TemporaryString, "mesh path")
			
			rt = CreateRoomTemplate(StrTemp)
			rt\Name = Lower(TemporaryString)
			
			StrTemp = Lower(GetINIString(file, TemporaryString, "shape"))
			
			Select StrTemp
				Case "room1", "1"
					rt\Shape = ROOM1
				Case "room2", "2"
					rt\Shape = ROOM2
				Case "room2c", "2c"
					rt\Shape = ROOM2C
				Case "room3", "3"
					rt\Shape = ROOM3
				Case "room4", "4"
					rt\Shape = ROOM4
				Default
			End Select
			
			For i = 0 To 4
				rt\zone[i]= GetINIInt(file, TemporaryString, "zone"+(i+1))
			Next
			
			rt\Commonness = Max(Min(GetINIInt(file, TemporaryString, "commonness"), 100), 0)
			rt\Large = GetINIInt(file, TemporaryString, "large")
			rt\DisableDecals = GetINIInt(file, TemporaryString, "disabledecals")
		EndIf
	Wend
	
;	i = 1
;	Repeat
;		StrTemp = GetINIString(file, "room ambience", "ambience"+i)
;		If StrTemp = "" Then Exit
;		
;		RoomAmbience[i]=LoadSound_Strict(StrTemp)
;		i=i+1
;	Forever
	
	CloseFile f
	
End Function

Function LoadRoomTemplateMeshes()
	Local rt.RoomTemplates
	
	For rt = Each RoomTemplates
		If rt\objPath<>""
			LoadRoomMesh(rt)
		EndIf
	Next
	
End Function

Function PlaceRoom(name$,x%,z%,angle%,zone%,shape%,event$="",eventchance#=1.0)
	Local rt.RoomTemplates,r.Rooms
	
	For rt = Each RoomTemplates
		If rt\Name = name$
			r = CreateRoom(zone,shape,x*8.0,0.0,z*8.0,name)
			RotateEntity(r\obj,0,angle,0)
			Exit
		EndIf
	Next
	
	r\event = event
	If r\event<>"" Then r\eventchance=eventchance
	
End Function

Global Mesh_MinX#,Mesh_MinY#,Mesh_MinZ#,Mesh_MaxX#,Mesh_MaxY#,Mesh_MaxZ#,Mesh_MagX#,Mesh_MagY#,Mesh_MagZ#

Function GetMeshExtents2(mesh)
	Local xmax#=-1000000
	Local xmin#= 1000000
	Local ymax#=-1000000
	Local ymin#= 1000000
	Local zmax#=-1000000
	Local zmin#= 1000000
	Local su,s,i,x#,y#,z#
	For su=1 To CountSurfaces(mesh)
		s=GetSurface(mesh,su)
		For i=0 To CountVertices(s)-1
			x#=VertexX(s,i)
			y#=VertexY(s,i)
			z#=VertexZ(s,i)
			TFormPoint x,y,z,mesh,0
			x=TFormedX()
			y=TFormedY()
			z=TFormedZ()
			If x>xmax Then xmax=x
			If x<xmin Then xmin=x
			If y>ymax Then ymax=y
			If y<ymin Then ymin=y
			If z>zmax Then zmax=z
			If z<zmin Then zmin=z
		Next
	Next
	
	Mesh_MinX = xmin
	Mesh_MinY = ymin
	Mesh_MinZ = zmin
	Mesh_MaxX = xmax
	Mesh_MaxY = ymax
	Mesh_MaxZ = zmax
	Mesh_MagX = xmax-xmin
	Mesh_MagY = ymax-ymin
	Mesh_MagZ = zmax-zmin
	
End Function

Function SetRoom(room_name$,room_type%,pos%,min_pos%,max_pos%) ;place a room without overwriting others
	
	If max_pos<min_pos Then DebugLog "Can't place "+room_name : Return False
	
	DebugLog "--- SETROOM: "+Upper(room_name)+" ---"
	Local looped%,can_place%
	looped = False
	can_place = True
	While MapRoom(room_type,pos)<>""
		DebugLog "found "+MapRoom(room_type,pos)
		pos=pos+1
		If pos>max_pos Then
			If looped=False Then
				pos=min_pos+1 : looped=True
			Else
				can_place=False
				Exit
			EndIf
		EndIf
	Wend
	DebugLog room_name+" "+Str(pos)
	If can_place=True Then
		DebugLog "--------------"
		MapRoom(room_type,pos)=room_name
		Return True
	Else
		DebugLog "couldn't place "+room_name
		Return False
	EndIf
End Function

Function GetZone(y%)
	Return Min(Floor((Float(MapWidth-y)/MapWidth*ZONEAMOUNT)),ZONEAMOUNT-1)
End Function

Type Props
	Field file$
	Field obj
End Type

Function CreatePropObj(file$)
	Local p.Props
	For p.Props = Each Props
		If p\file = file Then
			Return CopyEntity(p\obj)
		EndIf
	Next
	
	p.Props = New Props
	p\file = file
	p\obj = LoadMesh(file)
	Return p\obj
End Function

Type Rooms
	Field zone%
	
	Field found%
	
	Field obj%
	Field x#, y#, z#
	Field angle%
	Field RoomTemplate.RoomTemplates
	
	Field dist#
	
	Field overlapcheckbox%
	Field roomtype%
	
	Field overlaytex%
	Field resetoverlaytex%
	
	Field event$
	Field eventchance#
End Type 

Function CreateRoom.Rooms(zone%, roomshape%, x#, y#, z#, name$ = "")
	Local r.Rooms = New Rooms
	Local rt.RoomTemplates
	
	r\zone = zone
	
	r\roomtype = roomshape
	
	r\x = x : r\y = y : r\z = z
	
	If name <> "" Then
		name = Lower(name)
		For rt.RoomTemplates = Each RoomTemplates
			If rt\Name = name Then
				r\RoomTemplate = rt
				
				r\obj = CopyEntity(rt\obj)
				ScaleEntity(r\obj, RoomScale, RoomScale, RoomScale)
				;EntityPickMode(r\obj, 2)
				
				PositionEntity(r\obj, x, y, z)
				
				r\overlaytex = CreateTexture(1,1)
				SetBuffer TextureBuffer(r\overlaytex)
				ClsColor 0,0,0
				Cls
				SetBuffer BackBuffer()
				
				EntityTexture GetChild(r\obj,2),r\overlaytex,0,0
				
				Return r
			EndIf
		Next
	EndIf
	
End Function

Function CreateOverLapBox(r.Rooms)
	Local s
	Local sizeadd# = 0.02
	
	If r\RoomTemplate\Name = "exit1" Then Return
	If r\RoomTemplate\Name = "gatea" Then Return
	If r\RoomTemplate\Name = "room049" Then Return
	If r\RoomTemplate\Name = "room3storage" Then Return
	If r\RoomTemplate\Name = "room966" Then Return
	If r\RoomTemplate\Name = "gateaentrance" Then Return
	
	r\overlapcheckbox = CreateMesh()
	GetMeshExtents2(GetChild(r\obj,2))
	s = CreateSurface(r\overlapcheckbox)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MaxY-sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MaxY-sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MinY+sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MinY+sizeadd,Mesh_MinZ+sizeadd)
	AddTriangle s,0,1,2
	AddTriangle s,0,2,3
	s = CreateSurface(r\overlapcheckbox)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MaxY-sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MaxY-sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MinY+sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MinY+sizeadd,Mesh_MinZ+sizeadd)
	AddTriangle s,0,1,2
	AddTriangle s,0,2,3
	s = CreateSurface(r\overlapcheckbox)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MaxY-sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MaxY-sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MinY+sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MaxX-sizeadd,Mesh_MinY+sizeadd,Mesh_MaxZ-sizeadd)
	AddTriangle s,0,1,2
	AddTriangle s,0,2,3
	s = CreateSurface(r\overlapcheckbox)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MaxY-sizeadd,Mesh_MaxZ-sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MaxY-sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MinY+sizeadd,Mesh_MinZ+sizeadd)
	AddVertex(s,Mesh_MinX+sizeadd,Mesh_MinY+sizeadd,Mesh_MaxZ-sizeadd)
	AddTriangle s,0,1,2
	AddTriangle s,0,2,3
	EntityAlpha r\overlapcheckbox,0.5
	
End Function

Function LoadRoomMesh(rt.RoomTemplates)
	
	If Instr(rt\objPath,".rmesh")<>0 Then ;file is roommesh
		rt\obj = LoadRMesh(rt\objPath, rt)
	Else ;file is b3d
		;If rt\objPath <> "" Then rt\obj = LoadWorld(rt\objPath, rt) Else rt\obj = CreatePivot()
	EndIf
	
	If (Not rt\obj) Then RuntimeError "Failed to load map file "+Chr(34)+rt\objPath+Chr(34)+"."
	
	HideEntity(rt\obj)
	
End Function

Function StripFilename$(file$)
	Local mi$=""
	Local lastSlash%=0
	If Len(file)>0
		For i%=1 To Len(file)
			mi=Mid(file$,i,1)
			If mi="\" Or mi="/" Then
				lastSlash=i
			EndIf
		Next
	EndIf
	
	Return Left(file,lastSlash)
End Function

Type Materials
	Field name$
	Field Diff
	Field Bump
	
	Field StepSound%
End Type

Function LoadMaterials(file$)
	;If Not BumpEnabled Then Return
	
	Local TemporaryString$
	Local mat.Materials = Null
	Local StrTemp$ = ""
	
	Local f = OpenFile(file)
	
	While Not Eof(f)
		TemporaryString = Trim(ReadLine(f))
		If Left(TemporaryString,1) = "[" Then
			TemporaryString = Mid(TemporaryString, 2, Len(TemporaryString) - 2)
			
			mat.Materials = New Materials
			
			mat\name = Lower(TemporaryString)
			
;			If BumpEnabled Then
;				StrTemp = GetINIString(file, TemporaryString, "bump")
;				If StrTemp <> "" Then 
;					mat\Bump =  LoadTexture_Strict(StrTemp)
;					
;					TextureBlend mat\Bump, 6
;					TextureBumpEnvMat mat\Bump,0,0,-0.012
;					TextureBumpEnvMat mat\Bump,0,1,-0.012
;					TextureBumpEnvMat mat\Bump,1,0,0.012
;					TextureBumpEnvMat mat\Bump,1,1,0.012
;					TextureBumpEnvOffset mat\Bump,0.5
;					TextureBumpEnvScale mat\Bump,1.0				
;				EndIf
;			EndIf
			
			mat\StepSound = (GetINIInt(file, TemporaryString, "stepsound")+1)
		EndIf
	Wend
	
	CloseFile f
End Function

Function GetTextureFromCache%(name$)
	For tc.Materials=Each Materials
		If tc\name = name Then Return tc\Diff
	Next
	Return 0
End Function

Function GetBumpFromCache%(name$)
	For tc.Materials=Each Materials
		If tc\name = name Then Return tc\Bump
	Next
	Return 0
End Function

Function GetCache.Materials(name$)
	For tc.Materials=Each Materials
		If tc\name = name Then Return tc
	Next
	Return Null
End Function

Function AddTextureToCache(texture%)
	Local tc.Materials=GetCache(StripPath(TextureName(texture)))
	If tc.Materials=Null Then
		tc.Materials=New Materials
		tc\name=StripPath(TextureName(texture))
		If BumpEnabled Then
			Local temp$=GetINIString("Data\materials.ini",tc\name,"bump")
			If temp<>"" Then
				tc\Bump=LoadTexture(temp)
				TextureBlend tc\Bump,6
				TextureBumpEnvMat tc\Bump,0,0,-0.012
				TextureBumpEnvMat tc\Bump,0,1,-0.012
				TextureBumpEnvMat tc\Bump,1,0,0.012
				TextureBumpEnvMat tc\Bump,1,1,0.012
				TextureBumpEnvOffset tc\Bump,0.5
				TextureBumpEnvScale tc\Bump,1.0
			Else
				tc\Bump=0
			EndIf
		EndIf
		tc\Diff=0
	EndIf
	If tc\Diff=0 Then tc\Diff=texture
End Function

Function ClearTextureCache()
	For tc.Materials=Each Materials
		If tc\Diff<>0 Then FreeTexture tc\Diff
		If tc\Bump<>0 Then FreeTexture tc\Bump
		Delete tc
	Next
End Function

Function FreeTextureCache()
	For tc.Materials=Each Materials
		If tc\Diff<>0 Then FreeTexture tc\Diff
		If tc\Bump<>0 Then FreeTexture tc\Bump
		tc\Diff = 0 : tc\Bump = 0
	Next
End Function

Function LoadRMesh(file$,rt.RoomTemplates)
	;generate a texture made of white
	Local blankTexture%
	blankTexture=CreateTexture(4,4,1,1)
	ClsColor 255,255,255
	SetBuffer TextureBuffer(blankTexture)
	Cls
	SetBuffer BackBuffer()
	
	Local pinkTexture%
	pinkTexture=CreateTexture(4,4,1,1)
	ClsColor 255,255,255
	SetBuffer TextureBuffer(pinkTexture)
	Cls
	SetBuffer BackBuffer()
	
	ClsColor 0,0,0
	
	;read the file
	Local f%=ReadFile(file)
	Local i%,j%,k%,x#,y#,z#,yaw#
	Local vertex%
	Local temp1i%,temp2i%,temp3i%
	Local temp1#,temp2#,temp3#
	Local temp1s$, temp2s$
	
	Local collisionMeshes% = CreatePivot()
	
	Local hasTriggerBox% = False
	
	For i=0 To 3 ;reattempt up to 3 times
		If f=0 Then
			f=ReadFile(file)
		Else
			Exit
		EndIf
	Next
	If f=0 Then RuntimeError "Error reading file "+Chr(34)+file+Chr(34)
	Local isRMesh$ = ReadString(f)
	If isRMesh$="RoomMesh"
		;Continue
	ElseIf isRMesh$="RoomMesh.HasTriggerBox"
		hasTriggerBox% = True
	Else
		RuntimeError Chr(34)+file+Chr(34)+" is Not RMESH ("+isRMesh+")"
	EndIf
	
	file=StripFilename(file)
	
	Local count%,count2%
	
	;drawn meshes
	Local Opaque%,Alpha%
	
	Opaque=CreateMesh()
	Alpha=CreateMesh()
	
	count = ReadInt(f)
	Local childMesh%
	Local surf%,tex%[2],brush%
	
	Local isAlpha%
	
	Local u#,v#
	
	For i=1 To count ;drawn mesh
		childMesh=CreateMesh()
		
		surf=CreateSurface(childMesh)
		
		brush=CreateBrush()
		
		tex[0]=0 : tex[1]=0
		
		isAlpha=0
		For j=0 To 1
			temp1i=ReadByte(f)
			If temp1i<>0 Then
				temp1s=ReadString(f)
				tex[j]=GetTextureFromCache(temp1s)
				If tex[j]=0 Then ;texture is not in cache
					Select True
						Case temp1i<3
							tex[j]=LoadTexture(file+temp1s,1)
						Default
							tex[j]=LoadTexture(file+temp1s,3)
					End Select
					
					If tex[j]<>0 Then
						If temp1i=1 Then TextureBlend tex[j],5
						If Instr(Lower(temp1s),"_lm")<>0 Then
							TextureBlend tex[j],3
						EndIf
						AddTextureToCache(tex[j])
					EndIf
					
				EndIf
				If tex[j]<>0 Then
					isAlpha=2
					If temp1i=3 Then isAlpha=1
					
					TextureCoords tex[j],1-j
				EndIf
			EndIf
		Next
		
		If isAlpha=1 Then
			If tex[1]<>0 Then
				TextureBlend tex[1],2
				BrushTexture brush,tex[1],0,1
			Else
				BrushTexture brush,blankTexture,0,1
			EndIf
		Else
			If tex[0]<>0 And tex[1]<>0 Then
				For j=0 To 1
					BrushTexture brush,tex[j],0,j+2
				Next
				
				BrushTexture brush,AmbientLightRoomTex,1
			Else
				For j=0 To 1
					If tex[j]<>0 Then
						BrushTexture brush,tex[j],0,j+1
					Else
						BrushTexture brush,blankTexture,0,j+1
					EndIf
				Next
			EndIf
		EndIf
		
		surf=CreateSurface(childMesh)
		
		If isAlpha>0 Then PaintSurface surf,brush
		
		FreeBrush brush : brush = 0
		
		count2=ReadInt(f) ;vertices
		
		For j%=1 To count2
			;world coords
			x=ReadFloat(f) : y=ReadFloat(f) : z=ReadFloat(f)
			vertex=AddVertex(surf,x,y,z)
			
			;texture coords
			For k%=0 To 1
				u=ReadFloat(f) : v=ReadFloat(f)
				VertexTexCoords surf,vertex,u,v,0.0,k
			Next
			
			;colors
			temp1i=ReadByte(f)
			temp2i=ReadByte(f)
			temp3i=ReadByte(f)
			VertexColor surf,vertex,temp1i,temp2i,temp3i,1.0
		Next
		
		count2=ReadInt(f) ;polys
		For j%=1 To count2
			temp1i = ReadInt(f) : temp2i = ReadInt(f) : temp3i = ReadInt(f)
			AddTriangle(surf,temp1i,temp2i,temp3i)
		Next
		
		If isAlpha=1 Then
			AddMesh childMesh,Alpha
			EntityParent childMesh,collisionMeshes
			EntityAlpha childMesh,0.0
		Else
			AddMesh childMesh,Opaque
			EntityParent childMesh,collisionMeshes
			EntityAlpha childMesh,0.0
			;EntityType childMesh,HIT_MAP
			;EntityPickMode childMesh,2
			
			;make collision double-sided
			Local flipChild% = CopyMesh(childMesh)
			FlipMesh(flipChild)
			AddMesh flipChild,childMesh
			FreeEntity flipChild			
		EndIf
		
		
	Next
	
	Local hiddenMesh%
	hiddenMesh=CreateMesh()
	
	count=ReadInt(f) ;invisible collision mesh
	For i%=1 To count
		surf=CreateSurface(hiddenMesh)
		count2=ReadInt(f) ;vertices
		For j%=1 To count2
			;world coords
			x=ReadFloat(f) : y=ReadFloat(f) : z=ReadFloat(f)
			vertex=AddVertex(surf,x,y,z)
		Next
		
		count2=ReadInt(f) ;polys
		For j%=1 To count2
			temp1i = ReadInt(f) : temp2i = ReadInt(f) : temp3i = ReadInt(f)
			AddTriangle(surf,temp1i,temp2i,temp3i)
			AddTriangle(surf,temp1i,temp3i,temp2i)
		Next
	Next
	
	;trigger boxes
	If hasTriggerBox
		numb = ReadInt(f)
		For tb = 0 To numb-1
			count = ReadInt(f)
			For i%=1 To count
				count2=ReadInt(f)
				For j%=1 To count2
					ReadFloat(f) : ReadFloat(f) : ReadFloat(f)
				Next
				count2=ReadInt(f)
				For j%=1 To count2
					ReadInt(f) : ReadInt(f) : ReadInt(f)
				Next
			Next
			ReadString(f)
		Next
	EndIf
	
	count=ReadInt(f) ;point entities
	For i%=1 To count
		temp1s=ReadString(f)
		Select temp1s
			Case "screen"
				
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f)
				ReadString(f)
				
			Case "waypoint"
				
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f)
				
			Case "light"
				
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f) : ReadString(f) : ReadFloat(f)
				
			Case "spotlight"
				
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f) : ReadString(f) : ReadFloat(f) : ReadString(f) : ReadInt(f) : ReadInt(f)
				
			Case "soundemitter"
				
				ReadFloat(f)
				ReadFloat(f)
				ReadFloat(f)
				ReadInt(f)
				ReadFloat(f)
				
			Case "playerstart"
				
				ReadFloat(f) : ReadFloat(f) : ReadFloat(f)
				ReadString(f)
				
			Case "model"
				file = ReadString(f)
				If file<>""
					Local model = CreatePropObj("GFX\Map\Props\"+file) Or CreatePropObj("ProjectR\GFX\Map\Props\"+file);LoadMesh("GFX\Map\Props\"+file)
					
					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
					PositionEntity model,temp1,temp2,temp3
					
					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
					RotateEntity model,temp1,temp2,temp3
					
					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
					ScaleEntity model,temp1,temp2,temp3
					
					EntityParent model,Opaque
					EntityType model,HIT_MAP
					;EntityPickMode model,2
				Else
					DebugLog "file = 0"
					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
					DebugLog temp1+", "+temp2+", "+temp3
					
					;Stop
				EndIf

			Case "ladder"
				
				count2=ReadInt(f)
				
				If ladder = 0 Then ladder = CreateMesh() : EntityAlpha ladder,0
				
				surf = CreateSurface(ladder)
				
				For j=1 To count2
					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
					AddVertex surf,temp1,temp2,temp3
				Next
				
				count2=ReadInt(f)
				
				For j=1 To count2
					temp1i=ReadInt(f) : temp2i=ReadInt(f) : temp3i=ReadInt(f)
					AddTriangle(surf,temp1i,temp2i,temp3i)
				Next
								
		End Select
	Next
	
	Local obj%
	
	temp1i=CopyMesh(Alpha)
	FlipMesh temp1i
	AddMesh temp1i,Alpha
	FreeEntity temp1i
	
	If brush <> 0 Then FreeBrush brush
	
	AddMesh Alpha,Opaque
	FreeEntity Alpha
	
	EntityFX Opaque,3
	
	EntityAlpha hiddenMesh,0.0
	EntityAlpha Opaque,1.0
	
	;EntityType Opaque,HIT_MAP
	;EntityType hiddenMesh,HIT_MAP
	FreeTexture blankTexture
	
	;AddMesh hiddenMesh,BigRoomMesh
	
	obj=CreatePivot()
	CreatePivot(obj) ;skip "meshes" object
	EntityParent Opaque,obj
	EntityPickMode Opaque,2
	EntityParent hiddenMesh,obj
	CreatePivot(obj) ;skip "pointentites" object
	CreatePivot(obj) ;skip "solidentites" object
	EntityParent collisionMeshes,obj
	
	CloseFile f
	
	Return obj%
	
End Function

Function StripPath$(file$) 
	Local name$=""
	If Len(file$)>0 
		For i=Len(file$) To 1 Step -1 
			
			mi$=Mid$(file$,i,1) 
			If mi$="\" Or mi$="/" Then Return name$
			
			name$=mi$+name$ 
		Next 
		
	EndIf 
	
	Return name$ 
End Function

Function Piece$(s$,entry,char$=" ")
	While Instr(s,char+char)
		s=Replace(s,char+char,char)
	Wend
	For n=1 To entry-1
		p=Instr(s,char)
		s=Right(s,Len(s)-p)
	Next
	p=Instr(s,char)
	If p<1
		a$=s
	Else
		a=Left(s,p-1)
	EndIf
	Return a
End Function

Function KeyValue$(entity,key$,defaultvalue$="")
	properties$=EntityName(entity)
	properties$=Replace(properties$,Chr(13),"")
	key$=Lower(key)
	Repeat
		p=Instr(properties,Chr(10))
		If p Then test$=(Left(properties,p-1)) Else test=properties
		testkey$=Piece(test,1,"=")
		testkey=Trim(testkey)
		testkey=Replace(testkey,Chr(34),"")
		testkey=Lower(testkey)
		If testkey=key Then
			value$=Piece(test,2,"=")
			value$=Trim(value$)
			value$=Replace(value$,Chr(34),"")
			Return value
		EndIf
		If Not p Then Return defaultvalue$
		properties=Right(properties,Len(properties)-p)
	Forever 
End Function

Function GetINIString$(file$, section$, parameter$)
	Local TemporaryString$ = ""
	Local f = ReadFile(file)
	
	While Not Eof(f)
		If ReadLine(f) = "["+section+"]" Then
			Repeat 
				TemporaryString = ReadLine(f)
				If Trim( Left(TemporaryString, Max(Instr(TemporaryString,"=")-1,0)) ) = parameter Then
					CloseFile f
					Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
				EndIf
			Until Left(TemporaryString,1) = "[" Or Eof(f)
			CloseFile f
			Return ""
		EndIf
	Wend
	
	CloseFile f
End Function

Function GetINIInt%(file$, section$, parameter$)
	Local strtemp$ = Lower(GetINIString(file$, section$, parameter$))
	
	Select strtemp
		Case "true"
			Return 1
		Case "false"
			Return 0
		Default
			Return Int(strtemp)
	End Select
	Return 
End Function

Function GetINIFloat#(file$, section$, parameter$)
	Return GetINIString(file$, section$, parameter$)
End Function

Function PutINIValue%(INI_sAppName$, INI_sSection$, INI_sKey$, INI_sValue$)
	
; Returns: True (Success) or False (Failed)
	
	INI_sSection = "[" + Trim$(INI_sSection) + "]"
	INI_sUpperSection$ = Upper$(INI_sSection)
	INI_sKey = Trim$(INI_sKey)
	INI_sValue = Trim$(INI_sValue)
	INI_sFilename$ = CurrentDir$() + "\"  + INI_sAppName
	
; Retrieve the INI data (if it exists)
	
	INI_sContents$= INI_FileToString(INI_sFilename)
	
; (Re)Create the INI file updating/adding the SECTION, KEY and VALUE
	
	INI_bWrittenKey% = False
	INI_bSectionFound% = False
	INI_sCurrentSection$ = ""
	
	INI_lFileHandle = WriteFile(INI_sFilename)
	If INI_lFileHandle = 0 Then Return False ; Create file failed!
	
	INI_lOldPos% = 1
	INI_lPos% = Instr(INI_sContents, Chr$(0))
	
	While (INI_lPos <> 0)
		
		INI_sTemp$ =Trim$(Mid$(INI_sContents, INI_lOldPos, (INI_lPos - INI_lOldPos)))
		
		If (INI_sTemp <> "") Then
			
			If Left$(INI_sTemp, 1) = "[" And Right$(INI_sTemp, 1) = "]" Then
				
				; Process SECTION
				
				If (INI_sCurrentSection = INI_sUpperSection) And (INI_bWrittenKey = False) Then
					INI_bWrittenKey = INI_CreateKey(INI_lFileHandle, INI_sKey, INI_sValue)
				End If
				INI_sCurrentSection = Upper$(INI_CreateSection(INI_lFileHandle, INI_sTemp))
				If (INI_sCurrentSection = INI_sUpperSection) Then INI_bSectionFound = True
				
			Else
				
				; KEY=VALUE
				
				lEqualsPos% = Instr(INI_sTemp, "=")
				If (lEqualsPos <> 0) Then
					If (INI_sCurrentSection = INI_sUpperSection) And (Upper$(Trim$(Left$(INI_sTemp, (lEqualsPos - 1)))) = Upper$(INI_sKey)) Then
						If (INI_sValue <> "") Then INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
						INI_bWrittenKey = True
					Else
						WriteLine INI_lFileHandle, INI_sTemp
					End If
				End If
				
			End If
			
		End If
		
		; Move through the INI file...
		
		INI_lOldPos = INI_lPos + 1
		INI_lPos% = Instr(INI_sContents, Chr$(0), INI_lOldPos)
		
	Wend
	
	; KEY wasn't found in the INI file - Append a new SECTION if required and create our KEY=VALUE line
	
	If (INI_bWrittenKey = False) Then
		If (INI_bSectionFound = False) Then INI_CreateSection INI_lFileHandle, INI_sSection
		INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
	End If
	
	CloseFile INI_lFileHandle
	
	Return True ; Success
	
End Function

Function INI_FileToString$(INI_sFilename$)
	
	INI_sString$ = ""
	INI_lFileHandle% = ReadFile(INI_sFilename)
	If INI_lFileHandle <> 0 Then
		While Not(Eof(INI_lFileHandle))
			INI_sString = INI_sString + ReadLine$(INI_lFileHandle) + Chr$(0)
		Wend
		CloseFile INI_lFileHandle
	End If
	Return INI_sString
	
End Function

Function INI_CreateSection$(INI_lFileHandle%, INI_sNewSection$)
	
	If FilePos(INI_lFileHandle) <> 0 Then WriteLine INI_lFileHandle, "" ; Blank line between sections
	WriteLine INI_lFileHandle, INI_sNewSection
	Return INI_sNewSection
	
End Function

Function INI_CreateKey%(INI_lFileHandle%, INI_sKey$, INI_sValue$)
	
	WriteLine INI_lFileHandle, INI_sKey + "=" + INI_sValue
	Return True
	
End Function

Function Min#(a#,b#)
	If a < b Then Return a Else Return b
End Function

Function Max#(a#,b#)
	If a > b Then Return a Else Return b
End Function

Function Button%(x,y,width,height,txt$, disabled%=False)
	Local Pushed = False
	
	Color ClrR, ClrG, ClrB
	If Not disabled Then 
		If MouseX() > x*ResFactor And MouseX() < (x+width)*ResFactor Then
			If MouseY() > y*ResFactor And MouseY() < (y+height)*ResFactor Then
				If MouseDown1 Then
					Pushed = True
					Color ClrR*0.6, ClrG*0.6, ClrB*0.6
				Else
					Color Min(ClrR*1.2,255),Min(ClrR*1.2,255),Min(ClrR*1.2,255)
				EndIf
			EndIf
		EndIf
	EndIf
	
	If Pushed Then 
		Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor
		Color 133,130,125
		Rect (x+1)*ResFactor,(y+1)*ResFactor,(width-1)*ResFactor,(height-1)*ResFactor,False	
		Color 10,10,10
		Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor,False
		Color 250,250,250
		Line x*ResFactor,(y+height-1)*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor
		Line (x+width-1)*ResFactor,y*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor
	Else
		Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor
		Color 133,130,125
		Rect x*ResFactor,y*ResFactor,(width-1)*ResFactor,(height-1)*ResFactor,False	
		Color 250,250,250
		Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor,False
		Color 10,10,10
		Line x*ResFactor,(y+height-1)*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor
		Line (x+width-1)*ResFactor,y*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor		
	EndIf
	
	Color 255,255,255
	If disabled Then Color 70,70,70
	Text (x+width/2)*ResFactor,(y+height/2-1)*ResFactor, txt, True, True
	
	Color 0,0,0
	
	If Pushed And MouseHit1 Then PlaySound ButtonSFX : Return True
End Function

Function InputBox$(x,y,width,height,Txt$,ID=0)
	TextBox(x,y,width,height,Txt$)
	
	Local MouseOnBox = False
	
	If MouseX() > x*ResFactor And MouseX() < (x+width)*ResFactor Then
		If MouseY() > y*ResFactor And MouseY() < (y+height)*ResFactor Then
			MouseOnBox = True
			If MouseHit1 Then SelectedTextBox = ID : FlushKeys
		EndIf
	EndIf	
	
	If MouseOnBox = False And MouseHit1 And SelectedTextBox = ID Then SelectedTextBox = 0
	
	If SelectedTextBox = ID Then
		Txt = rInput(Txt)
		Color 0,0,0
		If (MilliSecs() Mod 800) < 400 Then  Rect ((x+width/2 + StringWidth(Txt)/2 + 2))*ResFactor, (y+height/2-5)*ResFactor, 2*ResFactor, 12*ResFactor
	EndIf
	
	Return Txt
End Function

Function TextBox(x,y,width,height,Txt$)
	Color 255,255,255
	Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor
	
	Color 128,128,128
	Rect x*ResFactor,y*ResFactor,width*ResFactor,height*ResFactor,False
	Color 64,64,64
	Rect (x+1)*ResFactor,(y+1)*ResFactor,(width-2)*ResFactor,(height-2)*ResFactor,False	
	Color 255,255,255
	Line (x+width-1)*ResFactor,y*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor
	Line x*ResFactor,(y+height-1)*ResFactor,(x+width-1)*ResFactor,(y+height-1)*ResFactor
	Color 212, 208, 199
	Line (x+width-2)*ResFactor,(y+1)*ResFactor,(x+width-2)*ResFactor,(y+height-2)*ResFactor
	Line (x+1)*ResFactor,(y+height-2)*ResFactor,(x+width-2)*ResFactor,(y+height-2)*ResFactor
	
	Color 0,0,0
	Text (x+width/2)*ResFactor,(y+height/2)*ResFactor, Txt, True, True
End Function

Function rInput$(aString$)
	value = GetKey()
	length = Len(aString$)
	If value = 8 Then value = 0 :If length > 0 Then aString$ = Left$(aString,Length-1)
	If value = 13 Then Goto ende
	If value = 0 Then Goto ende
	If value>0 And value<7 Or value>26 And value<32 Or value=9 Then Goto ende
	aString$=aString$ + Chr$(value)
	.ende
	Return aString$
End Function

Function MilliSecs2()
	Local retVal% = MilliSecs()
	If retVal < 0 Then retVal = retVal + 2147483648
	Return retVal
End Function






;~IDEal Editor Parameters:
;~F#14E#159#164#19B#1A6#1B8#1DF#1FE#202#207#215#22B#250#27D#28B#29A#2A2#2CA#2D1#2D8
;~F#2DF#2F8#300#308#455#465#476#48C#4A1#4AF#4B3#503#511#519#520#524#528#556#56D#580
;~F#58C
;~C#Blitz3D