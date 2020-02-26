import pandas as pd
import argparse
import plotly.graph_objs as go
import plotly
import math
import numpy as np


df = pd.read_csv("deal.txt", sep='\t', index_col=0)
data = []
groups = df.groupby("kind")
for name, group in groups:
    values=group["data"]
    bar = go.Bar(
        x=group.index,
        y=-np.log10(values),
        name=name,
    )
    data.append(bar)
layout = go.Layout(
    width=900,
    height=700,
    title="Bar Plot",
    xaxis=dict(
        showgrid=False,
        ticks='outside',
        zeroline=False,
        showline=True,
        tickangle=45,
    ),
    yaxis=dict(
        title="-log10(P)",
        showgrid=False,
        zeroline=False,
        ticks='outside',
        showline=True,
    ),
    hovermode="closest",
    margin=dict(
        b=250
    )
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