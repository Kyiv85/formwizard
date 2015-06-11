SELECT SUM(DQF) as Total from ( SELECT distinct dimensiondestino, dqf 
from tmp_red.dbo.asignacionTemporal  
WHERE  _IDPeriodo = 'A-2010'  
and _TipoModuloABCFuente = 'ACTIVITY' 
and _NombreConductor = '% CAP_10 a CAP_29'  
and FuenteDimMemberRef1='RR_RE_ORI_ANZOATEGUI' 
AND FuenteDimMemberRef2='PR_FI_ASECAS_O' 
AND   _TipoModuloABCDestino = 'COSTOBJECT') as tabla1


---------CAMBIO AGREGANDO EL ISNULL
--- APLICAR EN MÓDULO FUNCIONAL, ESTADISTICOS, POR REVISAR, Anzoategui-Acceso Discado _Unired Ras 
SELECT ISNULL(SUM(DQF),0) as Total from ( SELECT distinct dimensiondestino, dqf 
from tmp_red.dbo.asignacionTemporal  
WHERE  _IDPeriodo = 'A-2010'  
and _TipoModuloABCFuente = 'ACTIVITY' 
and _NombreConductor = '% CAP_10 a CAP_29'  
and FuenteDimMemberRef1='RR_RE_ORI_ANZOATEGUI' 
AND FuenteDimMemberRef2='PR_FI_ASECAS_O' 
AND   _TipoModuloABCDestino = 'COSTOBJECT') as tabla1