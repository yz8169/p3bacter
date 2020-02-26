$.ajaxSetup(
    {
        cache: false,
    }
)

var zhRunning = "正在运行"
var zhInfo = "信息"

function progressHandlingFunction(e) {
    if (e.lengthComputable) {
        var percent = e.loaded / e.total * 100;
        $('#progress').html("(" + percent.toFixed(2) + "%)");
        if (percent >= 100) {
            $("#info").text("正在运行")
            $("#progress").html("")
        }
    }
}

function fileExist() {
    var b = false
    $(":file").each(function (i, v) {
        var isvisible = $(this).is(":visible")
        if (isvisible) {
            b = true
            return false
        }
    })
    return b
}

function getNow(s) {
    return s < 10 ? '0' + s : s;
}

function getCurrentTime() {
    var myDate = new Date();
//获取当前年
    var year = myDate.getFullYear();
//获取当前月
    var month = myDate.getMonth() + 1;
//获取当前日
    var date = myDate.getDate();
    var h = myDate.getHours();       //获取当前小时数(0-23)
    var m = myDate.getMinutes();     //获取当前分钟数(0-59)
    var s = myDate.getSeconds();

    var now = year + '_' + getNow(month) + "_" + getNow(date) + "_" + getNow(h) + '_' + getNow(m) + "_" + getNow(s);
    return now
}

function inputSelect(element) {
    $(element).select()
}

function refreshMissionName() {
    $("input[name='missionName']").val(getCurrentTime())
    $("#form").bootstrapValidator("revalidateField", "missionName")
}


var isIE = (function () {
    var browser = {};
    return function (ver, c) {
        var key = ver ? (c ? "is" + c + "IE" + ver : "isIE" + ver) : "isIE";
        var v = browser[key];
        if (typeof (v) != "undefined") {
            return v;
        }
        if (!ver) {
            v = (navigator.userAgent.indexOf('MSIE') !== -1 || navigator.appVersion.indexOf('Trident/') > 0);
        } else {
            var match = navigator.userAgent.match(/(?:MSIE |Trident\/.*; rv:|Edge\/)(\d+)/);
            if (match) {
                var v1 = parseInt(match[1]);
                v = c ? (c == 'lt' ? v1 < ver : (c == 'gt' ? v1 > ver : undefined)) : v1 == ver;
            } else if (ver <= 9) {
                var b = document.createElement('b')
                var s = '<!--[if ' + (c ? c : '') + ' IE ' + ver + ']><i></i><![endif]-->';
                b.innerHTML = s;
                v = b.getElementsByTagName('i').length === 1;
            } else {
                v = undefined;
            }
        }
        browser[key] = v;
        return v;
    };
}());

var zhRunning = "正在运行"
var zhInfo = "信息"
var num = 3

function extractor(query) {
    var result = /([^\n]+)$/.exec(query);
    if (result && result[1])
        return result[1].trim();
    return '';
}

var matcherRegex = /[^\n]*$/
var matcherEnd = "\n"

$(function () {

    $("[data-toggle='popover']").popover()

})




