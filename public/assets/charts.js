$('document').ready(function() {


    var ctx = $("#chartCanvas")[0].getContext('2d');


      $.get('crime/type/percentage', function() { alert("succes");}).done(function (data) {
        var types = new Array();
        var percentages= new Array();

        console.log(data);
        for(i=0; i < data.length; i++){
            types.push(data[i].Type);
            percentages.push(data[i].Percentage);
        }

          var lineChart = new Chart(ctx, {
              type: 'bar',
              data: {
                  labels: types,
                  datasets: [{
                      label: '# %',
                      data: percentages,
                      borderWidth: 1
                  }]
              },
              options: {
                  title : {display: true,
                      text:'Crime Types'},
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