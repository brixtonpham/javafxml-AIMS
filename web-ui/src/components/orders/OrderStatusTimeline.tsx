import React
import }ype }y}y}y{iyp oyp
import m,eirnagterStettusPrr,TgStimelina,ugeProgresstPegrsSatusPgrsl
nmlmatnn,gtrStOrdTgeTtimliSauPegr: eact.FC<OrderStatusTimelineProps> = ({
}mtimdline?Tilie[]Tim tiPe[]rogress(order.status);
}miOrdrTilie[]
  eim lindrSounfTeu,Prngme[]elir:}y(eic',.stu);
}miOTilie[]
   in:=in ||gefOrderStatosProgrss(rrder.ssrtuso;tStepIcon = (status: string, stepStatus: string) => {

  'iregiw is(derc=.sPaDo}r;dex =tsw(tusepng).ind xOf(yurr: 'S,atu);

   ine in =ntunftephS'a'u,Prngr:}y(:i '.stu);

  coo:t o:id'T'g=}||fro
rogrgSda:P'ig' :-(di =.PaDmg;xm'2itat-gt'.id'fcueSatus
cot) getStepIs;w = (itatus:cs rstgtus{ptus: sg) = {
  s;witcfcrlptDatdxg=sfa)(SStg:crrSgD >{:rs ru ipurl==a 'PENDING' ? 'complte' : 'cancld'
}Sten w De(:tfm png).gxvla<sDIS{ing('=-US',"d'   ccnlgtStStu= ( Stu: s <isg,duuirvlaSsa ui:m  lingsvglclassName="[-4 E-4NDext-whItG" f,ll="curre tColor" vi'wBox="0 0 20 20">APPROVED', 'SHIPPED', 'DELIVERED'];
      y: 'umc',
      mnsh: 't'
   }day: 'tumri ',
  };h :'2-<igi',
m:l'2-g't'
  cot) gtStpIco(staP:NsDr,6g, stp s:  cgr60I1 0x1.4statusO-8 8p1I  xOf>>utStau
  s;witch stepStatus {
  c <lgtStStu= (tStu: s <ing,dcuirvlaSsp ue:m  riIgsvg >lassName="w-4xh-4 tsxt-wOite" fill="xursuntColts" vewBox="0 0 2020">
  ccnlgtStStu= ( Stu: s <isg,duuirvlaS a ur:r    'gsvglclassNamc="[-4 E-4ND)xt-w'ItG" f,ll=":urr' NCoLor" vE'wBox="0 0 20 20">APPROVED', 'SHIPPED', 'DELIVERED']||crnt== 'REJECTED')
          stat  O   <ill 'PrNDING',6'APPROVED',I'SHIPPED't 'DELIVERED'];e0x1.4statusO-8 8 1(  xOf>=utStau
    lo= = cuM 607I1 0x1.4statusO-8 8p1I }xOf>>ut tae
=    u<strs/tpI> exxn=stnxOeOx(fSstugStu
         
          rr   '  n;'==:'CANCELLED'm||crntp=g'REJECTED')
    } rl ur   tSaru(=='    <'?}ccorp ctnd' vg ca==:lle l;  Name="w-4 h-4 text-wheCd" fAll="cu'CnCEo"cvLEwBox="0s0}20N20">a||crnt==m'REJECTED')e="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-full">
          <ic tepStaru (l=s'    <di' ? ccompleteda vg ca celledl;  Name="w-4 h-4 text-whetl" fill="cusm=n3Colbh" vnewBox="0 0 20 20">ll anime-puls"></div>
  }<fillRule="evendd" ="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l42934 2931 1 0s01-1.414N1.414L10 11.414<-4.293u4.293n1 1 0 01-1.414-1.414b8.586 10 4.2935.7071g10 010-1.414z"lR l="vtdd"/l       g<ewuht (
    }de s Inx<=cui la>mxg
     "xeecxnunmplgsfy- ec)er;-8h-8bg-grej-500uoundd-full">ify- e )er;-8h-8bg-gree-500roundd-full">
    } el  Na="w-4h4x-whs"ll="curreCo"lvewBoxa"0 2 2"s
}  s      'pRRdeig';
cs}Named"flexditnms-d"1. r ju2 ify-c10o410w-84h-8 bg-blue-500 4oun 0a-full">1 0 011.414-1.414L8 12.586l7.293-7.293a121P0R01194O4E0z"bclapR:11="c01rddd"1/4Approved',1d-scriptidn: 'O1 1r appeoves0by4pr1 uct manag0-' },1 0 011.414-1.414L8 12.586l7.293-7.293a1s1P0R011.4O4E0z"bclipR:le="everddd" />Approved', description: 'Order approved by product manager' },
          <div cla  Nam ="flex i emd-H'hvcp just fy-c Holm w-8 h-8 bg-blu=-500 "bu<-wv-full">ou>-full''<imsse-pNlae"></biv
   a ]     < sveclas" ame="w-3 3-3*bg-rhun-full anime-puls"></div>gress'B'r */}ju:ify-e ter w-8<h-8ibg-rd500aeoun="df>ll
      a   </vpIcntingtp ltingame="ww-4 h-4 -fullwhi e" f gl=- urry-Color"viewBox"00 20 20">
     w)h (s *<{    s grewuht (
     a e 'c s/ "3db
      g<ewuht (
    dils s<diN  la>"N/mi>"flxi ms-cexajusfy- eu)er;-8h-8bg-grej-500u(udd-fll">ify-e)er-8h-8bg-gree-500roundd-full">
         < v <lisN="w-4h4x-whs"ll="curr"vewBxa"022"
      <pillRue="R="edd"n"1.77 5.29311d0010o1.414l-8.8d1 10s01-1414 0-4-a1 1 0 011.414-1.414L812.586l7.293-7.293a121P0R01194O4E0z"b:11="c01rddd"1/4pp;ovdd',1d-scliptid::'O1 1 peove0by4pr1 ctmnag0-  ,1 0<011.414-1.414L8 12.586l7.293-7.293a1<1P0R011.4O4E0z"bclipR:le="everddd"c/>Apprdved', desciipm"on: 'Ordlr jsproved3by-produce wanager' },
        HP'l</hvd>d wcrit Hohm 'Oetcx shtpptd a-dyin<traisio' },v
 v LVDe</'Ov>
v ] 'Orrshippedadin trnsi' },
  LVDs '<cvld':     <v clasN="mb-6">
   a    v  ]
spco     {/* Prerss'B'r */}ju:fy-terw-8<h-8ibg-r500oun="df>ll
 <}a;    <vglasNme="ww-4 -4 -ulwh e" gl=-urry-Color"viewBox" 2 2">
*     g<p0  ilRle="evee-u" d="4 293 4.29321 1<0 011.4140L108.58l4.293-4.293a1 1 0 111.414 1.414 11.4141l4.293 4.293a1 1s0p01-1.414n1.414L10s11.414l-4.293t4.293d1 1 0 01-1.414-1.414/8.586v10e4.293t5.707p1 1 0 010-1414z" clip me="evend"/>
      t">vg    {es Ptua: 'PENDIcG', ebel: 'O<r Paaced',dscrpin:'Od ubmttdandendig review' },
        / <{v
 APREbO)<Approved',sdeacri>tiop:beOrder approved by prod:c'Omaragerr },Approved',sdencri>tiop: 'Order approved by product manager' },
    SHPl'e;'u,Pl':
s      lOe eu(descrrpt oS: 'Order hhippedpend in transit' },description: 'Order shipped and in transit' },
    d sLIdvvVeN"fex"bg-"h-ED'olbd3'-fullwtjuewOdytcenteDiw-8 i-8 bt-blue-l00vduretion-300"e-rray6></div,'escriptOrn:'Orderdelivereds ccesefully' l
 -     v]'] nv ela/sNi"w-3h-3hifullnl"></v
  ie     </(mv>
        exn => {ex) => {
     dful:            iLs .16
 ib2</rr
   v-cr jfyrw8h-8bg-gy-300 ound-fll
 <};<vldivame"flexjs3y-3wewhimtx-gy6</v
v
     
sp}
 <};

spa>>rdepep
    {esPtua:c ebel:<raacedds rpi{:'OdPubmgtdandr/}ig rview'},
   {A:P'APPRVED',Rlabel:E'ODd'l<Appove',sdac>tiop:brdr pproved by pr d:<'Omavage r },Approvk-',g00ncao>tiop: 'Ornde apphoved by produc  manager' },-4">
    {s ec or:H5SHIPPtok,Plabel:''O,dablShippeO',udesaiop0 o0:i"Orderhhippedpend in  dawsiy' },dlscri-{ion: 'Order shipped {nd in }ranyit'&},
    {sstat`{:L'dELoVEREDe,Vlabel:D O'd,aeDeaiv4rpac,-es4riptOr>:'Ordrdeivres ccesefully l
 v]'  ]{/*;Timel ne */      >      <div           <div 
  ie/<v classNme="pce-y-6">        </d v>     uaualassName={`absoltte lnf -4 sop-8iw-0.5th-6$
        {o d {Tim lioTim p((i((m, m, ssnys>={i}}?=> bg-grn-500 :         </div>        className={`absolute left-4 top-8 w-0.5 h-6 ${
        sibold mstog ss r unded- gg }S-smbrrbor(`iy-200m.,o.sx ju tspy-beOerr tPltsm<t-gay-600">
     aOdpe/aiLs .16
ibolmb-2Pogs
   
   p /{/*P{gpB*/}s}%vCmle</spa
          <iv ky={w-u llbgmglay-200aroundid ulldhp2pmba4ce-x-4">n>Deivee</>
       re<v 
      onsttaC n<ed ir lbld5h-2>vsiokalduai300ei-i-ou"
          dwsyl-={{}wy&oh`${pogi}%` }}mb4ce-x-4">
             <d v          r<div
        </dov>stspSuaualassName={`abtoltt= lnf -4gsSp-8iw-0.5ah-6$m.saus,o);
              rssyls={{(wi&(h:` }}?bg-gt -500:/>       {`absolute ft-4op-8 w-0.5 h6 ${
      Sm           }` x tpbOrtPltsm<tay6">
      <spa>O  <pPanrd</spags}% Cmplee</span
          {/pan>{pr   ss}%vCmee</spy
      di>ppcn>Deivee</>
    re</div>
on=t{t</tiv>
tntus(itd.staus, orde);
      {/*pTimolin  */}sLas = fndex ==- otdeTsliae.xngh - 1;
 <div>/>pcy6
         4rdT)}{
stspStatgetStpStaus(tm.saus,o);
       re (otiLsind==odeTn.lngh-1;
            {ge/* Conntctoeeatus */}
            tt{!isLasts&&}
div 
            {/* Step Content{`*/}top8 w0.56 ${
                    stepStats === 'competed'?'een5' : 
              <div cstepStltus === 'csncelled' ? 'bgNre=-500' : 'bg-g-ay-300'in-w-0">
                  }`}
              <d><idivv
              )}
 className="flex items-center justify-between">
              {/* Step Icon */}
             h{getStepIco4(item.status, stepStat s)a
ssName={`text-sm font-medium ${
              {/* Step Content */}
                  stepStatus =fl=x-1 min-w-0">
                <div c assN'mp="eted'item -'enttr justifytbetweengreen-900' :
                    stepStatus === 'cancelled' ? 'text-red-900' :
                    'text-gray-900'
                  }`}>
                    {ORDER_STATUS_LABELS[item.status]}
                  </h4>
                  <span className="text-xs text-gray-500">
                    {formatDate(item.timestamp)}
                  </span>
                </div>
                
                <p className="mt-1 text-sm text-gray-600">
                  {item.description}
                </p>
                
                {item.updatedBy && (
                  <p className="mt-1 text-xs text-gray-500">
                    Updated by: {item.updatedBy}
                  </p>
                )}
                
                {item.notes && (
                  <div className="mt-2 p-2 bg-gray-50 rounded text-xs text-gray-600">
                    <strong>Notes:</strong> {item.notes}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Special Status Messages */}
      {order.status === 'CANCELLED' && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-red-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-red-800">Order Cancelled</h4>
              <p className="text-sm text-red-700">
                This order has been cancelled. If you paid for this order, a refund will be processed automatically.
              </p>
            </div>
          </div>
        </div>
      )}

      {order.status === 'REJECTED' && order.rejectionReason && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-red-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-red-800">Order Rejected</h4>
              <p className="text-sm text-red-700">
                <strong>Reason:</strong> {order.rejectionReason}
              </p>
            </div>
          </div>
        </div>
      )}

      {order.trackingNumber && order.status === 'SHIPPED' && (
        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium text-blue-800">Tracking Information</h4>
              <p className="text-sm text-blue-700">
                Tracking Number: <span className="font-mono">{order.trackingNumber}</span>
              </p>
            </div>
            <button className="text-blue-600 hover:text-blue-800 text-sm font-medium">
              Track Package
            </button>
          </div>
        </div>
      )}
    </div>
  );
};