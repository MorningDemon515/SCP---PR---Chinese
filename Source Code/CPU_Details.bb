Graphics 640,480,0,2
SetBuffer BackBuffer()

SetFont LoadFont("GFX\font\cour\Morning Demon.ttf")

Cls

Color 255,255,255
Text 0,0,"此程序显示您的CPU详细信息（以前用于调试hud）."
Color 255,0,0
Text 0,20,"警告：此程序不适用于所有用户,"
Text 0,40,"这就是为什么它在原始游戏的exe中被删除的原因."
Color 255,255,255
Text 0,80,"按任意键继续."

Flip

WaitKey()

Global kCPUid$, kCPUfamily%, kCPUsteppingId%, kCPUbrand$, kCPUextendedId$, kCPUfeatures$

kCPUid$         = CPUid$()
kCPUfamily%     = CPUfamily%()
kCPUsteppingId% = CPUsteppingId%()
kCPUbrand$      = CPUbrand$()
kCPUextendedId$ = CPUextendedId$()
kCPUfeatures$   = CPUfeatures$()

Repeat
	Cls
	Color 255,255,255
	Text 0,0,LSet("CPU ID: ",18)+kCPUid
	Text 0,20,LSet("CPU 系列: ",18)+kCPUfamily
	Text 0,40,LSet("CPU 步进 ID: ",18)+kCPUsteppingId
	Text 0,60,LSet("CPU 品牌: ",18)+kCPUbrand
	Text 0,80,LSet("CPU 名称: ",18)+kCPUextendedId
	Text 0,100,LSet("CPU 功能: ",18)+kCPUfeatures
	Text 0,140,"按任意键关闭这个程序."
	Flip
	Delay 8
Until WaitKey()
End
;~IDEal Editor Parameters:
;~C#Blitz3D