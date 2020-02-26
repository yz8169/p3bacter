import numpy as np
import pandas as pd
import plotly
import plotly.graph_objs as go
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-g", type=str, default="", help="gene name")
parser.add_argument("-s", type=str, default=[], nargs='+', help="sample name")
args = parser.parse_args()

df = pd.read_csv("deal.txt", sep='\t', index_col=0)
if args.g != "":
    g = args.g.split(";")
    df = df[df.index.isin(g)]
if args.s:
    df = df[args.s]
trace = go.Heatmap(z=df.values.tolist(),
                   x=df.columns,
                   y=df.index)
data = [trace]
layout = go.Layout(
    title="Correlation Coefficient Heatmap",
    width=800,
    height=800,
    xaxis=dict(
        showgrid=False,
        zeroline=False,
        title=""
    ),
    yaxis=dict(
        showgrid=False,
        zeroline=False,
    ),
    hovermode="closest",
    margin=go.Margin(
        l=120
    )
)
fig = dict(data=data, layout=layout)
config = dict(
    displaylogo=False,
    showLink=False,
    # modeBarButtonsToRemove=["sendDataToCloud"],
)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", output_type="div", include_plotlyjs=False, config=config)
f = open('div.txt', 'w')
f.write(a)
f.close()
