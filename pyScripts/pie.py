import pandas as pd
import argparse
import plotly.graph_objs as go
import plotly


df = pd.read_csv("deal.txt", sep='\t', index_col=0)
columnNames=df.columns
plotRow=df.ix["data"]
bar = go.Pie(
    labels=columnNames,
    values=plotRow,
)
data = [bar]
layout = go.Layout(
    width=1000,
    height=400,
    title="Pie Plot",
    xaxis=dict(
        showgrid=False,
        ticks='outside',
        zeroline=False,
        showline=True,
    ),
    yaxis=dict(
        title="Exp",
        showgrid=False,
        zeroline=False,
        ticks='outside',
        showline=True,
    ),
    hovermode="closest",
)
config = dict(
    displaylogo=False,
    showLink=False,
    modeBarButtonsToRemove=["sendData"],
    sendData=False,
    staticPlot=False,

)
fig = dict(data=data, layout=layout)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()