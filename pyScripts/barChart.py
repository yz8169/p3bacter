import pandas as pd
import argparse
import plotly.graph_objs as go
import plotly

parser = argparse.ArgumentParser()
parser.add_argument("-g", type=str, default="", help="will plot gene id")
args = parser.parse_args()

df = pd.read_csv("deal.txt", sep='\t', index_col=0)
columnNames=df.columns
plotRow=df.ix[args.g]
bar = go.Bar(
    x=columnNames,
    y=plotRow,
)
data = [bar]
layout = go.Layout(
    title="Bar Plot",
    xaxis=dict(
        title="Protein:"+args.g,
        showgrid=False,
        ticks='outside',
        zeroline=False,
        showline=True,
    ),
    yaxis=dict(
        title="Expression",
        showgrid=False,
        zeroline=False,
        ticks='outside',
        showline=True,
    ),
    hovermode="closest",
    width="1000",
    height="500"
)
config = dict(
    displaylogo=False,
    showLink=False,
)
fig = dict(data=data, layout=layout)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()