$('document').ready(function() {


    var ctxCrimeDev = $("#crimeDev")[0].getContext('2d');
    var ctxCrimeType = $("#crimeType")[0].getContext('2d');
      $.get('crime/year/deviation',function () { }).done(function (data) {
          var types = new Array();
          var percentages= new Array();

          console.log(data);
          for(i=0; i < data.length; i++){
              types.push(data[i].Year);
              percentages.push(data[i].Percentage);
          }

          var lineChart = new Chart(ctxCrimeDev, {
              type: 'line',
              data: {
                  labels: types,
                  datasets: [{
                      label: '%',
                      backgroundColor: 'rgba(255, 99, 132, 0.2)',
                      borderColor: 'rgba(255,99,132,1)',
                      data: percentages,
                      fill : true
                  }]
              },
              options: {
                  title : {display: true,
                      text:'Crime Percentage Deviation'},
                  onResize : function () { console.log("resized .. "); },
                  scales: {
                      yAxes: [{
                          ticks: {
                              beginAtZero: true
                          }
                      }]
                  }
              }
          });
      });
      $.get('crime/type/percentage', function() { }).done(function (data) {
        var types = new Array();
        var percentages= new Array();

        console.log(data);
        for(i=0; i < data.length; i++){
            types.push(data[i].Type);
            percentages.push(data[i].Percentage);
        }

          var lineChart = new Chart(ctxCrimeType, {
              type: 'bar',
              data: {
                  labels: types,
                  datasets: [{
                      label: '%',
                      data: percentages,
                  backgroundColor: [
                      'rgba(255, 99, 132, 0.8)',
                      'rgba(255, 99, 132, 0.7)',
                      'rgba(255, 99, 132, 0.6)',
                      'rgba(255, 99, 132, 0.5)',
                      'rgba(255, 99, 132, 0.4)',
                      'rgba(255, 99, 132, 0.3)',
                      'rgba(255, 99, 132, 0.2)',
                      'rgba(255, 99, 132, 0.1)',
                      'rgba(255, 99, 132, 0.1)',
                      'rgba(255, 99, 132, 0.05)'
                  ],
                  borderColor: [
                      'rgba(255,99,132,1)',
                      'rgba(255,99,132,0.95)',
                      'rgba(255,99,132,0.9)',
                      'rgba(255,99,132,0.85)',
                      'rgba(255,99,1320.8)',
                      'rgba(255,99,132,0.75)',
                      'rgba(255,99,132,0.7)',
                      'rgba(255,99,132,0.65)',
                      'rgba(255,99,132,0.6)',
                      'rgba(255,99,132,0.55)',
                  ],
                  borderWidth: 1
                  }]
              },
              options: {
                  title : {display: true },
                  onResize : function () { console.log("resized .. "); },
                  scales: {
                      yAxes: [{
                          ticks: {
                              beginAtZero: true
                          }
                      }]
                  }
              }
          });
    });

});